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
}

- (void)connection:(ContextualConnection)connection didFailWithError:(id)error
{
  if (errorAction) {
    if (callerContext) {
      [delegate performSelector:errorAction withObject:error withObject:callerContext];
    } else {
      [delegate performSelector:errorAction withObject:error];
    }
  }
}

- (void)connection:(ContextualConnection)connection didReceiveResponse:(CPHTTPURLResponse)response
{
  statusCode = [response statusCode];
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
    if (errorAction) {
      if (callerContext) {
        [delegate performSelector:errorAction withObject:data withObject:callerContext];
      } else {
        [delegate performSelector:errorAction withObject:data];
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

@implementation RemoteService : CPObject
{
}

+ (RemoteService)sharedInstance
{
  if (sharedInstance == nil) {
    sharedInstance = [[RemoteService alloc] init];
  }
  return sharedInstance;
}

+ (void)get:(CPString)requestUrl delegate:(id)delegate onSuccess:(SEL)action onError:(SEL)errorAction context:(id)callerContext
{
  var context = [[RemoteServiceContext alloc] init];
  [context setDelegate:delegate];
  [context setAction:action];
  [context setErrorAction:errorAction];
  [context setCallerContext:callerContext];
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

+ (void)post:(JSObject)object to:(CPString)requestUrl delegate:(id)delegate onSuccess:(SEL)action onError:(SEL)errorAction context:(id)callerContext
{
  var context = [[RemoteServiceContext alloc] init];
  [context setDelegate:delegate];
  [context setAction:action];
  [context setErrorAction:errorAction];
  [context setCallerContext:callerContext];
  [RequestHelper asyncPost:requestUrl object:object context:context delegate:context];
}

- (void)post:(JSObject)object to:(CPString)requestUrl delegate:(id)delegate onSuccess:(SEL)action onError:(SEL)errorAction context:(id)callerContext
{
  [RemoteService post:object to:requestUrl delegate:delegate onSuccess:action onError:errorAction context:callerContext];
}

@end
