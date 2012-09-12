@implementation CrysonError : CPObject
{
  CPString   message @accessors;
  CPNumber   statusCode @accessors;
  CPArray    validationFailures @accessors;
}

+ (CrysonError)errorWithMessage:(CPString)message statusCode:(CPNumber)statusCode validationFailures:(CPArray)validationFailures
{
  var error = [[CrysonError alloc] init];
  [error setMessage:message];
  [error setStatusCode:statusCode];
  [error setValidationFailures:validationFailures];
  return error;
}

@end

@implementation CrysonValidationFailure : CPObject
{
  CrysonEntity     entity @accessors;
  CPString         keyPath @accessors;
  id               value @accessors;
  CPString         message @accessors;
}

+ (CrysonValidationFailure)validationFailureWithEntity:(CrysonEntity)entity keyPath:(CPString)keyPath value:(id)value message:(CPString)message
{
  var validationFailure = [[CrysonValidationFailure alloc] init];
  [validationFailure setEntity:entity];
  [validationFailure setKeyPath:keyPath];
  [validationFailure setValue:value];
  [validationFailure setMessage:message];
  return validationFailure;
}

@end
