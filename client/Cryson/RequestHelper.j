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

@import <Foundation/Foundation.j>
@import "ContextualConnection.j"

// Perform synchronous or asynchronous HTTP GET/POST/PUT/DELETE requests against server.
// Results and POST bodies are expected to be JSON.

@implementation RequestHelper : CPObject
{
}

+ (JSObject)syncGet:(CPString)url
{
  return [self syncRequestWithVerb:"GET" url:url object:nil];
}

+ (JSObject)syncPost:(CPString)url object:(JSObject)object
{
  return [self syncRequestWithVerb:"POST" url:url object:object];
}

+ (JSObject)syncPut:(CPString)url object:(JSObject)object
{
  return [self syncRequestWithVerb:"PUT" url:url object:object];
}

+ (JSObject)syncDelete:(CPString)url object:(JSObject)object
{
  return [self syncRequestWithVerb:"DELETE" url:url object:object];
}

+ (void)asyncGet:(CPString)url context:(id)context delegate:(id)delegate
{
  [self asyncRequestWithVerb:"GET" url:url object:nil context:context delegate:delegate];
}

+ (void)asyncPost:(CPString)url object:(JSObject)object context:(id)context delegate:(id)delegate
{
  [self asyncPost:url object:object context:context delegate:delegate customHeaders:nil];
}

+ (void)asyncPost:(CPString)url object:(JSObject)object context:(id)context delegate:(id)delegate customHeaders:(CPDictionary)customHeaders
{
  [self asyncRequestWithVerb:"POST" url:url object:object context:context delegate:delegate customHeaders:customHeaders];
}

+ (void)asyncPut:(CPString)url object:(JSObject)object context:(id)context delegate:(id)delegate
{
  [self asyncRequestWithVerb:"PUT" url:url object:object context:context delegate:delegate];
}

+ (void)asyncDelete:(CPString)url object:(JSObject)object context:(id)context delegate:(id)delegate
{
  [self asyncRequestWithVerb:"DELETE" url:url object:object context:context delegate:delegate];
}

@end

@implementation RequestHelper (Private)

+ (JSObject)syncRequestWithVerb:(CPString)verb url:(CPString)url object:(JSObject)object
{
  var request = [CPURLRequest requestWithURL:url];
  [request setHTTPMethod:verb];
  [request setValue:"application/json" forHTTPHeaderField:"Accept"];
  [request setValue:"application/json" forHTTPHeaderField:"Content-Type"];
  if (object) {
    [request setHTTPBody:JSON.stringify(object)];
  }

  var response = [CPURLConnection sendSynchronousRequest:request returningResponse:nil];
  if (response && [response rawString]) {
    try {
      return [[response rawString] objectFromJSON];
    } catch(ex) {
      if(console && console.error) {
        console.error("RequestHelper#syncRequestWithVerb:url:object: failed to convert response from "+url+" to JSON.", response);
      }
    }
  }
  return nil;
}

+ (void)asyncRequestWithVerb:(CPString)verb url:(CPString)url object:(JSObject)object context:(id)context delegate:(id)delegate
{
  [self asyncRequestWithVerb:verb url:url object:object context:context delegate:delegate customHeaders:nil];
}

+ (void)asyncRequestWithVerb:(CPString)verb url:(CPString)url object:(JSObject)object context:(id)context delegate:(id)delegate customHeaders:(CPDictionary) customHeaders
{
  var request = [CPURLRequest requestWithURL:url];
  [request setHTTPMethod:verb];
  [request setValue:"application/json" forHTTPHeaderField:"Accept"];
  [request setValue:"application/json" forHTTPHeaderField:"Content-Type"];
  if(customHeaders) {
    var keys = [customHeaders allKeys];
    for(var i = 0; i < [keys count]; i++) {
      var key = [keys objectAtIndex:i];
      var value = [customHeaders valueForKey:key];
      [request setValue:value forHTTPHeaderField:key];
    }

  }
  if (object) {
    [request setHTTPBody:JSON.stringify(object)];
  }

  var connection = [[ContextualConnection alloc] initWithRequest:request
                                                 delegate:delegate
                                                 startImmediately:YES
                                                 context:context];
}

@end
