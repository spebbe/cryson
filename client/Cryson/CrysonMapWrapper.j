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

@implementation CrysonMapWrapper : CPObject
{
  CrysonEntity parentEntity;
  CPString parentAttributeName;
  JSObject attributes @accessors;
}

- (id)initWithParentEntity:(CrysonEntity)aParentEntity parentAttributeName:(CPString)aParentAttributeName andAttributes:(JSObject)someAttributes
{
  self = [super init];
  if (self) {
    parentEntity = aParentEntity;
    parentAttributeName = aParentAttributeName;
    attributes = someAttributes;
  }
  return self;
}

- (void)replaceEntries:(CrysonMapWrapper)aMapWrapper
{
  attributes = _.clone([aMapWrapper attributes]);
}

- (JSObject)toJSObject
{
  return attributes;
}

- (CPMethodSignature)methodSignatureForSelector:(SEL)selector {
    return YES;
}

- (void)forwardInvocation:(CPInvocation)anInvocation {
  var aSelector = [anInvocation selector];
  var selectorString = CPStringFromSelector(aSelector);
  if (selectorString.indexOf('set') == 0 && selectorString.indexOf(':')!=-1) {
    var unSettedSelectorString = selectorString.replace(/^set/, '').replace(':', '');
    var attributeName = unSettedSelectorString.charAt(0).toLowerCase() + unSettedSelectorString.slice(1);
    [anInvocation setReturnValue:[self _setAttribute:attributeName toValue:[anInvocation argumentAtIndex:2]]];
  } else {
    [anInvocation setReturnValue:[self _getAttribute:selectorString]];
  }
}

- (void)setValue:(id)aValue forKey:(CPString)aKey
{
  var selector = CPSelectorFromString('set' + aKey.charAt(0).toUpperCase() + aKey.substr(1) + ':');
  if ([self respondsToSelector:selector]) {
    [self performSelector:selector withObject:aValue];
    return;
  }
  [self _setAttribute:aKey toValue:aValue];
}

- (id)valueForKey:(CPString)aKey
{
  var selector = CPSelectorFromString(aKey);
  if ([self respondsToSelector:selector]) {
    return [self performSelector:selector];
  }
  return [self _getAttribute:aKey];
}

- (id)_setAttribute:(CPString)attributeName toValue:(id)attributeValue
{
  [parentEntity willChangeValueForKey:"dirty"];
  [parentEntity willChangeValueForKey:parentAttributeName];
  [self willChangeValueForKey:attributeName];
  attributes[attributeName] = attributeValue;
  [self didChangeValueForKey:attributeName];
  [parentEntity didChangeValueForKey:parentAttributeName];
  [parentEntity didChangeValueForKey:"dirty"];
  return self;
}

- (id)_getAttribute:(CPString)attributeName
{
  return attributes[attributeName];
}

@end
