import json
import boto3
import requests
import os
from datetime import datetime, timedelta

def lambda_handler(event, context):
    """
    Lambda function to check for expiring SSL certificates and send notifications
    
    Args:
        event: CloudWatch Events trigger
        context: Lambda context
        
    Returns:
        dict: Response with status and message
    """
    
    # Get configuration from environment variables
    api_endpoint = os.environ.get('API_ENDPOINT', 'http://localhost:8080/api/v1')
    sns_topic_arn = os.environ.get('SNS_TOPIC_ARN')
    alert_days = int(os.environ.get('ALERT_DAYS', '30'))
    
    try:
        # Query the API for expiring certificates
        response = requests.get(f'{api_endpoint}/certificates/expiring?days={alert_days}')
        response.raise_for_status()
        
        expiring_certificates = response.json()
        
        if expiring_certificates and len(expiring_certificates) > 0:
            # Send notification via SNS
            sns = boto3.client('sns')
            
            # Create detailed message
            message_body = create_alert_message(expiring_certificates, alert_days)
            
            sns.publish(
                TopicArn=sns_topic_arn,
                Subject=f'SSL Certificate Expiry Alert - {len(expiring_certificates)} certificates expiring within {alert_days} days',
                Message=message_body
            )
            
            print(f"Alert sent for {len(expiring_certificates)} expiring certificates")
            
            return {
                'statusCode': 200,
                'body': json.dumps({
                    'message': f'Alert sent for {len(expiring_certificates)} expiring certificates',
                    'certificate_count': len(expiring_certificates),
                    'timestamp': datetime.utcnow().isoformat()
                })
            }
        else:
            print("No certificates expiring within the specified timeframe")
            return {
                'statusCode': 200,
                'body': json.dumps({
                    'message': f'No certificates expiring within {alert_days} days',
                    'certificate_count': 0,
                    'timestamp': datetime.utcnow().isoformat()
                })
            }
            
    except requests.exceptions.RequestException as e:
        error_msg = f"Failed to connect to API: {str(e)}"
        print(error_msg)
        return {
            'statusCode': 500,
            'body': json.dumps({
                'error': error_msg,
                'timestamp': datetime.utcnow().isoformat()
            })
        }
    except Exception as e:
        error_msg = f"Unexpected error: {str(e)}"
        print(error_msg)
        return {
            'statusCode': 500,
            'body': json.dumps({
                'error': error_msg,
                'timestamp': datetime.utcnow().isoformat()
            })
        }

def create_alert_message(certificates, alert_days):
    """
    Create a formatted alert message for SNS
    
    Args:
        certificates: List of expiring certificates
        alert_days: Number of days for alert threshold
        
    Returns:
        str: Formatted message
    """
    
    message = f"""
SSL Certificate Expiry Alert

Found {len(certificates)} certificate(s) expiring within {alert_days} days.

Certificate Details:
"""
    
    for cert in certificates:
        domain = cert.get('domain', 'Unknown')
        expiry_date = cert.get('expiryDate', 'Unknown')
        days_until_expiry = cert.get('daysUntilExpiry', 'Unknown')
        
        message += f"""
- Domain: {domain}
  Expiry Date: {expiry_date}
  Days Until Expiry: {days_until_expiry}
"""
    
    message += f"""
Generated at: {datetime.utcnow().strftime('%Y-%m-%d %H:%M:%S UTC')}

Please take immediate action to renew these certificates to avoid service disruption.
"""
    
    return message

def test_local():
    """
    Test function for local development
    """
    # Set test environment variables
    os.environ['API_ENDPOINT'] = 'http://localhost:8080/api/v1'
    os.environ['SNS_TOPIC_ARN'] = 'arn:aws:sns:us-east-1:123456789012:test-topic'
    os.environ['ALERT_DAYS'] = '30'
    
    # Mock event and context
    event = {}
    context = type('Context', (), {'function_name': 'test'})()
    
    result = lambda_handler(event, context)
    print(json.dumps(result, indent=2))

if __name__ == "__main__":
    test_local() 