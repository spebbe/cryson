/*
  Cryson
  
  Copyright 2011-2012 Björn Sperber (cryson@sperber.se)
  
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
  http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

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
