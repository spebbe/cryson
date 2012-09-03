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

@implementation CrysonEntityAsyncProxy : CPObject
{
  CrysonEntity        entity;
  BOOL                withinAsyncOperation @accessors;
  CPMutableSet        loadingAssociations; // TODO: Replace. CPSet is slow.
}

- (id)initWithEntity:(CrysonEntity)anEntity
{
  self = [super init];
  if (self) {
    entity = anEntity;
    loadingAssociations = [[CPMutableSet alloc] init];
  }
  return self;
}

- (BOOL)respondsToSelector:(SEL)aSelector
{
  return [entity respondsToSelector:aSelector];
}

- (CPMethodSignature)methodSignatureForSelector:(SEL)selector {
  return [entity methodSignatureForSelector:selector];
}

- (void)forwardInvocation:(CPInvocation)anInvocation {
  withinAsyncOperation = YES;
  [entity forwardInvocation:anInvocation];
  withinAsyncOperation = NO;
}

- (id)valueForKey:(CPString)aKey
{
  withinAsyncOperation = YES;
  var returnValue = [entity valueForKey:aKey];
  withinAsyncOperation = NO;
  return returnValue;
}

- (void)setValue:(id)aValue forKey:(CPString)aKey
{
  [entity setValue:aValue forKey:aKey];
}

- (id)loadAssociation:(CPString)associationName byClass:(CLASS)associationClass andIds:(CPArray)associationId
{
  if (![loadingAssociations containsObject:associationName]) {
    [loadingAssociations addObject:associationName];
    [[entity session] findByClass:associationClass andIds:associationId fetch:nil delegate:[self delegateWithAssociationName:associationName]];
  }
  return [];
}

- (id)loadAssociation:(CPString)associationName byClass:(CLASS)associationClass andId:(CPNumber)associationId
{
  if (![loadingAssociations containsObject:associationName]) {
    [loadingAssociations addObject:associationName];
    [[entity session] findByClass:associationClass andId:associationId fetch:nil delegate:[self delegateWithAssociationName:associationName]];  
  }
  return nil;
}

- (CrysonEntityAsyncProxyDelegate)delegateWithAssociationName:(CPString)anAssociationName
{
  return [[CrysonEntityAsyncProxyDelegate alloc] initWithEntity:entity associationName:anAssociationName];
}

@end

@implementation CrysonEntityAsyncProxyDelegate : CPObject
{
  CrysonEntity entity;
  CPString     associationName;
}

- (id)initWithEntity:(CrysonEntity)anEntity associationName:(CPString)anAssociationName
{
  self = [super init];
  if (self) {
    entity = anEntity;
    associationName = anAssociationName;
  }
  return self;
}

- (void)crysonSession:(CrysonSession)aSession found:(CrysonEntity)anEntity byClass:(CLASS)klazz
{
  [entity setValue:anEntity forKey:associationName];
}

- (void)crysonSession:(CrysonSession)aSession found:(CPArray)someEntities byClass:(CLASS)klazz andIds:(CPArray)someIds
{
  [entity setValue:someEntities forKey:associationName];
}

@end
