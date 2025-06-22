import pytest
import json
import os
import sys
from unittest.mock import patch, MagicMock

# Add the parent directory to the path to import the lambda function
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from certificate_alert import lambda_handler, create_alert_message

class TestCertificateAlert:
    
    def test_lambda_handler_success_with_expiring_certificates(self):
        """Test successful execution with expiring certificates"""
        
        # Mock environment variables
        with patch.dict(os.environ, {
            'API_ENDPOINT': 'http://localhost:8080/api/v1',
            'SNS_TOPIC_ARN': 'arn:aws:sns:us-east-1:123456789012:test-topic',
            'ALERT_DAYS': '30'
        }):
            
            # Mock requests.get to return expiring certificates
            mock_response = MagicMock()
            mock_response.json.return_value = [
                {
                    'domain': 'example.com',
                    'expiryDate': '2024-04-01T00:00:00Z',
                    'daysUntilExpiry': 25
                },
                {
                    'domain': 'test.com',
                    'expiryDate': '2024-04-15T00:00:00Z',
                    'daysUntilExpiry': 39
                }
            ]
            mock_response.raise_for_status.return_value = None
            
            # Mock SNS client
            mock_sns = MagicMock()
            
            with patch('requests.get', return_value=mock_response), \
                 patch('boto3.client', return_value=mock_sns):
                
                # Call the lambda handler
                result = lambda_handler({}, {})
                
                # Verify the result
                assert result['statusCode'] == 200
                body = json.loads(result['body'])
                assert body['message'] == 'Alert sent for 2 expiring certificates'
                assert body['certificate_count'] == 2
                
                # Verify SNS was called
                mock_sns.publish.assert_called_once()
                call_args = mock_sns.publish.call_args
                assert call_args[1]['TopicArn'] == 'arn:aws:sns:us-east-1:123456789012:test-topic'
                assert 'SSL Certificate Expiry Alert' in call_args[1]['Subject']
    
    def test_lambda_handler_success_no_expiring_certificates(self):
        """Test successful execution with no expiring certificates"""
        
        # Mock environment variables
        with patch.dict(os.environ, {
            'API_ENDPOINT': 'http://localhost:8080/api/v1',
            'SNS_TOPIC_ARN': 'arn:aws:sns:us-east-1:123456789012:test-topic',
            'ALERT_DAYS': '30'
        }):
            
            # Mock requests.get to return empty list
            mock_response = MagicMock()
            mock_response.json.return_value = []
            mock_response.raise_for_status.return_value = None
            
            with patch('requests.get', return_value=mock_response):
                
                # Call the lambda handler
                result = lambda_handler({}, {})
                
                # Verify the result
                assert result['statusCode'] == 200
                body = json.loads(result['body'])
                assert body['message'] == 'No certificates expiring within 30 days'
                assert body['certificate_count'] == 0
    
    def test_lambda_handler_api_error(self):
        """Test handling of API errors"""
        
        # Mock environment variables
        with patch.dict(os.environ, {
            'API_ENDPOINT': 'http://localhost:8080/api/v1',
            'SNS_TOPIC_ARN': 'arn:aws:sns:us-east-1:123456789012:test-topic',
            'ALERT_DAYS': '30'
        }):
            
            # Mock requests.get to raise an exception
            with patch('requests.get', side_effect=Exception('API Error')):
                
                # Call the lambda handler
                result = lambda_handler({}, {})
                
                # Verify the result
                assert result['statusCode'] == 500
                body = json.loads(result['body'])
                assert 'error' in body
                assert 'Unexpected error' in body['error']
    
    def test_create_alert_message(self):
        """Test alert message creation"""
        
        certificates = [
            {
                'domain': 'example.com',
                'expiryDate': '2024-04-01T00:00:00Z',
                'daysUntilExpiry': 25
            },
            {
                'domain': 'test.com',
                'expiryDate': '2024-04-15T00:00:00Z',
                'daysUntilExpiry': 39
            }
        ]
        
        message = create_alert_message(certificates, 30)
        
        # Verify message content
        assert 'SSL Certificate Expiry Alert' in message
        assert 'Found 2 certificate(s) expiring within 30 days' in message
        assert 'example.com' in message
        assert 'test.com' in message
        assert '25' in message
        assert '39' in message
        assert 'Please take immediate action' in message
    
    def test_create_alert_message_empty_list(self):
        """Test alert message creation with empty certificate list"""
        
        message = create_alert_message([], 30)
        
        # Verify message content
        assert 'SSL Certificate Expiry Alert' in message
        assert 'Found 0 certificate(s) expiring within 30 days' in message

if __name__ == '__main__':
    pytest.main([__file__]) 