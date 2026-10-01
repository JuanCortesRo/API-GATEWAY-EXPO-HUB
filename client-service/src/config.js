import { Amplify } from 'aws-amplify';

Amplify.configure({
  Auth: {
    Cognito: {
      userPoolId: 'us-east-1_WyUxPZ3hF',
      userPoolClientId: '5mn8kpjmjahao9ru6b6ms99s9i',
      region: 'us-east-1'
    }
  }
});
