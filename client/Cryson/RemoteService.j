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

@import "RequestHelper.j"

@implementation RemoteServiceContext : CPObject
{
  CPNumber statusCode @accessors;
  id delegate @accessors;
  SEL action @accessors;
  SEL errorAction @accessors;
  id callerContext @accessors;

  // For retry
  CPString verb @accessors;
  CPString requestUrl @accessors;
  id requestObject @accessors;
  CPNumber retryCount @accessors;
}

- (JSFunction)retryBlock
{
  return function() {
    if ([self verb] == @"GET") {
      [self setRetryCount:[self retryCount] + 1];
      [RequestHelper asyncGet:[self requestUrl]
                      context:self
                     delegate:self];
    } else if ([self verb] == @"POST") {
      [self setRetryCount:[self retryCount] + 1];
      [RequestHelper asyncPost:[self requestUrl]
                        object:[self requestObject]
                       context:self
                      delegate:self];
    }
  }
}

- (void)connection:(ContextualConnection)connection didFailWithError:(id)error
{
  if (retryHandler && ([retryHandler willRetryRemoteServiceContext:self data:error statusCode:statusCode retryBlock:[self retryBlock]])) {
    return;
  }

  if (errorAction) {
    if (callerContext) {
      [delegate performSelector:errorAction withObjects:error,statusCode,callerContext];
    } else {
      [delegate performSelector:errorAction withObjects:error,statusCode];
    }
  }
}

- (void)connection:(ContextualConnection)connection didReceiveResponse:(CPHTTPURLResponse)response
{
  statusCode = (parseInt([response statusCode]) || 0);
}

- (void)connection:(ContextualConnection)connection didReceiveData:(CPString)data
{
  if (statusCode == 200) {
    if (callerContext) {
      [delegate performSelector:action withObject:[self fromJSON:data] withObject:callerContext];
    } else {
      [delegate performSelector:action withObject:[self fromJSON:data]];
    }
  } else {

    if (retryHandler && ([retryHandler willRetryRemoteServiceContext:self data:data statusCode:statusCode retryBlock:[self retryBlock]])) {
      return;
    }

    if (errorAction) {
      if (callerContext) {
        [delegate performSelector:errorAction withObjects:data,statusCode,callerContext];
      } else {
        [delegate performSelector:errorAction withObjects:data,statusCode];
      }
    }
  }
}

- (JSObject)fromJSON:(CPString)s
{
  if (s && s != "") {
    return [s objectFromJSON];
  }
  return nil;
}

@end

var sharedInstance = nil;

var retryHandler = nil;

@implementation RemoteService : CPObject
{
  CPDictionary customHeaders;
}

- (void)setCustomHeaders:(CPDictionary)someCustomHeaders
{
  customHeaders = someCustomHeaders;
}

- (void)setCustomHeader:(CPString)key withValue:(CPString)value
{
  [customHeaders setValue:value forKey:key];
}

+ (RemoteService)sharedInstance
{
  if (sharedInstance == nil) {
    sharedInstance = [[RemoteService alloc] init];
    [sharedInstance setCustomHeaders:[CPDictionary dictionary]];
  }
  return sharedInstance;
}

+ (void)setRetryHandler:(id)aRetryHandler
{
  retryHandler = aRetryHandler;
}

+ (void)get:(CPString)requestUrl delegate:(id)delegate onSuccess:(SEL)action onError:(SEL)errorAction context:(id)callerContext
{
  var context = [[RemoteServiceContext alloc] init];
  [context setDelegate:delegate];
  [context setAction:action];
  [context setErrorAction:errorAction];
  [context setCallerContext:callerContext];
  [context setVerb:@"GET"];
  [context setRequestUrl:requestUrl];
  [context setRequestObject:nil];
  [context setRetryCount:0];
  [RequestHelper asyncGet:requestUrl context:context delegate:context];
}

- (void)get:(CPString)requestUrl delegate:(id)delegate onSuccess:(SEL)action onError:(SEL)errorAction context:(id)callerContext
{
  [RemoteService get:requestUrl delegate:delegate onSuccess:action onError:errorAction context:callerContext];
}


+ (void)get:(CPString)requestUrl delegate:(id)delegate onSuccess:(SEL)action onError:(SEL)errorAction
{
  [RemoteService get:requestUrl delegate:delegate onSuccess:action onError:errorAction context:nil];
}

- (void)get:(CPString)requestUrl delegate:(id)delegate onSuccess:(SEL)action onError:(SEL)errorAction
{
  [RemoteService get:requestUrl delegate:delegate onSuccess:action onError:errorAction context:nil];
}

+ (void)get:(CPString)requestUrl delegate:(id)delegate onSuccess:(SEL)action
{
  [RemoteService get:requestUrl delegate:delegate onSuccess:action onError:nil context:nil];
}

- (void)get:(CPString)requestUrl delegate:(id)delegate onSuccess:(SEL)action
{
  [RemoteService get:requestUrl delegate:delegate onSuccess:action onError:nil context:nil];
}

+ (void)post:(JSObject)object to:(CPString)requestUrl delegate:(id)delegate onSuccess:(SEL)action onError:(SEL)errorAction context:(id)callerContext customHeaders:(CPDictionary)customHeaders
{
  var context = [[RemoteServiceContext alloc] init];
  [context setDelegate:delegate];
  [context setAction:action];
  [context setErrorAction:errorAction];
  [context setCallerContext:callerContext];
  [context setVerb:@"POST"];
  [context setRequestUrl:requestUrl];
  [context setRequestObject:object];
  [context setRetryCount:0];
  [RequestHelper asyncPost:requestUrl object:object context:context delegate:context customHeaders:customHeaders];
}

+ (void)post:(JSObject)object to:(CPString)requestUrl delegate:(id)delegate onSuccess:(SEL)action onError:(SEL)errorAction context:(id)callerContext
{
  [RemoteService post:object to:requestUrl delegate:delegate onSuccess:action onError:errorAction context:callerContext customHeaders:nil];
}

- (void)post:(JSObject)object to:(CPString)requestUrl delegate:(id)delegate onSuccess:(SEL)action onError:(SEL)errorAction context:(id)callerContext
{
  [RemoteService post:object to:requestUrl delegate:delegate onSuccess:action onError:errorAction context:callerContext customHeaders:customHeaders];
}

@end
