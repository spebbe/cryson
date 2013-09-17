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

@import "../Cryson/Cryson.j"
if (typeof (_) == 'undefined') {
  _ = exports._;
}
@import <OJMoq/OJMoq.j>
@import "TestEntity.j"

@implementation CrysonEntityTest : OJTestCase
{
}

- (void)testBasicInitialization
{
  var entity = [self givenTestEntityWithJSObject:{"id":1, "name":"test"}];

  [OJAssert assert:1 equals:[entity id]];
  [OJAssert assert:@"test" equals:[entity name]];
}

- (void)testNullAssociation
{
  var entity = [self givenTestEntityWithJSObject:{"id":1, "something_cryson_id":nil}];

  [OJAssert assert:nil equals:[entity something]];
}

- (void)testEmbeddedToOneAssociation
{
  var entity = [self givenTestEntityWithJSObject:
					   {"id":1,
					    "name":"outer",
					    "something":{"id":2,
							 "name":"inner",
							 "something":nil,
							 "crysonEntityClass":"TestEntity"}}];

  [OJAssert assert:@"outer" equals:[entity name]];
  [OJAssert assert:@"inner" equals:[[entity something] name]];
}

- (void)testEmbeddedToOneAssociation
{
  var entity = [self givenTestEntityWithJSObject:
					   {"id":1,
					    "name":"outer",
					    "something":{"id":2,
							 "name":"inner",
							 "something":nil,
							 "crysonEntityClass":"TestEntity"}}];

  [OJAssert assert:@"outer" equals:[entity name]];
  [OJAssert assert:@"inner" equals:[[entity something] name]];
}

- (void)testEmbeddedToOneAssociationWithUnauthorizedEntity
{
  var entity = [self givenTestEntityWithJSObject:
             {"id":1,
              "name":"outer",
              "something":{"id":2,
               "crysonEntityClass":"TestEntity",
               "crysonUnauthorized":true}}];

  [OJAssert assert:@"outer" equals:[entity name]];
  [OJAssert assertNull:[entity something]];
  [OJAssert assert:2 equals:[entity toJSObject].something_cryson_id];
}

- (void)testEmbeddedToManyAssociation
{
  var entity = [self givenTestEntityWithJSObject:
					   {"id":1,
					    "name":"outer",
					    "something":[{"id":2,
							 "name":"inner",
							 "something":nil,
							 "crysonEntityClass":"TestEntity"}]}];

  [OJAssert assert:@"outer" equals:[entity name]];
  [OJAssert assert:@"inner" equals:[[[entity something] objectAtIndex:0] name]];
}

- (void)testEmbeddedToManyAssociationWithUnauthorizedEntity
{
  var entity = [self givenTestEntityWithJSObject:
             {"id":1,
              "name":"outer",
              "something":[
                {"id":2,
                 "crysonEntityClass":"TestEntity",
                 "crysonUnauthorized":true},
                {"id":3,
                 "name":"inner",
                 "something":nil,
                 "crysonEntityClass":"TestEntity"}]}];

  [OJAssert assert:@"outer" equals:[entity name]];
  [OJAssert assert:1 equals:[entity countOfSomething]];
  [OJAssert assert:[2, 3] equals:[entity toJSObject].something_cryson_ids];
}

- (void)testLazilyResolvedToOneAssociation
{
  var innerEntity = [self givenTestEntityWithJSObject:
					   {"id":2,
					    "name":"inner",
					    "something_cryson_id":nil}];

  var entity = [self givenTestEntityWithJSObject:
					   {"id":1,
					    "name":"outer",
					    "something_cryson_id":2}];

  [[entity session] stubFindSyncByClass:TestEntity andId:2 returning:innerEntity];

  [OJAssert assert:@"outer" equals:[entity name]];
  [OJAssert assert:@"inner" equals:[[entity something] name]];
}

- (void)testLazilyResolvedToOneAssociationWithUnauthorizedEntity
{
  var entity = [self givenTestEntityWithJSObject:
             {"id":1,
              "name":"outer",
              "something_cryson_id":2}];

  [[entity session] stubFindSyncByClass:TestEntity andId:2
                              returning:[[CrysonUnauthorizedEntity alloc] initWithJSObject:{"id":2, "crysonUnauthorized":YES} session:[entity session]]];
  
  [OJAssert assert:@"outer" equals:[entity name]];
  [OJAssert assertNull:[entity something]];
  [OJAssert assert:2 equals:[entity toJSObject].something_cryson_id];
}

- (void)testLazilyResolvedToManyAssociation
{
  var innerEntity = [self givenTestEntityWithJSObject:
					   {"id":2,
					    "name":"inner",
					    "something_cryson_id":nil}];

  var entity = [self givenTestEntityWithJSObject:
					   {"id":1,
					    "name":"outer",
					    "something_cryson_ids":[2]}];

  [[entity session] stubFindSyncByClass:TestEntity andIds:[2] returning:[innerEntity]];

  [OJAssert assert:@"outer" equals:[entity name]];
  [OJAssert assert:@"inner" equals:[[[entity something] objectAtIndex:0] name]];
}

- (void)testLazilyResolvedToManyAssociationWithUnauthorizedEntity
{
  var innerEntity = [self givenTestEntityWithJSObject:
             {"id":2,
              "name":"inner",
              "something_cryson_id":nil}];

  var entity = [self givenTestEntityWithJSObject:
             {"id":1,
              "name":"outer",
              "something_cryson_ids":[2, 3]}];
  
  [[entity session] stubFindSyncByClass:TestEntity andIds:[2,3]
                              returning:[
                                         innerEntity,
                                          [[CrysonUnauthorizedEntity alloc] initWithJSObject:{"id":3, "crysonUnauthorized":YES} session:[entity session]]
                                         ]];
  
  [OJAssert assert:@"outer" equals:[entity name]];
  [OJAssert assert:@"inner" equals:[[[entity something] objectAtIndex:0] name]];
  [OJAssert assert:1 equals:[entity countOfSomething]];
  [OJAssert assert:[2,3] equals:[entity toJSObject].something_cryson_ids];
}

- (void)testDirtyCheck
{
  var entity = [self givenTestEntityWithJSObject:{"id":1, "name":"test"}];

  [OJAssert assert:@"test" equals:[entity name]];
  [OJAssert assertFalse:[entity dirty]];

  [entity setName:@"changed!"];

  [OJAssert assert:@"changed!" equals:[entity name]];
  [OJAssert assertTrue:[entity dirty]];
}

- (void)testRevert
{
  var entity = [self givenTestEntityWithJSObject:{"id":1, "name":"test"}];

  [entity setName:@"changed!"];

  [OJAssert assert:@"changed!" equals:[entity name]];

  [entity revert];

  [OJAssert assert:@"test" equals:[entity name]];
  [OJAssert assertFalse:[entity dirty]];
}

- (void)testBindings
{
  var entity = [self givenTestEntityWithJSObject:{"id":1, "name":"test"}];
  var listener = [self givenTestEntityWithJSObject:{"id":2, "name":"test"}];

  [listener bind:"name" toObject:entity withKeyPath:"name" options:nil];

  [entity setName:"test1"];
  [OJAssert assert:"test1" equals:[listener name]];

  [entity setName:"test2"];
  [OJAssert assert:"test2" equals:[listener name]];
}

- (void)testDirtyBindings
{
  var entity = [self givenTestEntityWithJSObject:{"id":1, "name":"test"}];
  var listener = [self givenTestEntityWithJSObject:{"id":2, "shouldBeDirty":"false"}];

  [listener bind:"shouldBeDirty" toObject:entity withKeyPath:"dirty" options:nil];

  [OJAssert assert:"false" equals:[listener shouldBeDirty]];

  [entity setName:"test1"];
  [OJAssert assert:"true" equals:[listener shouldBeDirty]];
}

- (void)testDirtyBindingsForToManyAssociations
{
  var entity = [self givenTestEntityWithJSObject:{"id":1, "something":[]}];
  var listener = [self givenTestEntityWithJSObject:{"id":2, "shouldBeDirty":"false"}];

  [listener bind:"shouldBeDirty" toObject:entity withKeyPath:"dirty" options:nil];

  [OJAssert assert:"false" equals:[listener shouldBeDirty]];

  [entity insertObject:entity inSomethingAtIndex:0];
  [OJAssert assert:"true" equals:[listener shouldBeDirty]];
}

@end

@implementation SessionMock : CPObject
{
  JSObject findSyncByClassAndIdStubs;
  JSObject findSyncByClassAndIdsStubs;
  JSObject definitionStubs;
}

- (id)init
{
  self = [super init];
  if (self) {
    findSyncByClassAndIdStubs = {};
    findSyncByClassAndIdsStubs = {};
    definitionStubs = {};
  }
  return self;
}

- (void)stubDefinitionForClass:(CLASS)aClass returning:(id)aDefinition
{
  definitionStubs[aClass] = aDefinition;
}

- (id)findDefinitionForClass:(CLASS)aClass
{
  return definitionStubs[aClass];
}

- (void)stubFindSyncByClass:(CLASS)aClass andId:(CPNumber)aNumber returning:(id)aReturnValue
{
  findSyncByClassAndIdStubs[[aClass, aNumber]] = aReturnValue;
}

- (void)stubFindSyncByClass:(CLASS)aClass andIds:(CPArray)someNumbers returning:(id)aReturnValue
{
  findSyncByClassAndIdsStubs[[aClass, someNumbers]] = aReturnValue;
}

- (id)findSyncByClass:(CLASS)aClass andId:(CPNumber)anId fetch:(CPArray)fetch
{
  return findSyncByClassAndIdStubs[[aClass,anId]];
}

- (id)findSyncByClass:(CLASS)aClass andIds:(CPArray)someIds fetch:(CPArray)fetch
{
  return findSyncByClassAndIdsStubs[[aClass,someIds]];
}

- (id)findCachedByClass:(CLASS)aClass andId:(CPNumber)anId
{
  return nil;
}

- (void)attach:(id)ignored
{
}

@end

@implementation CrysonEntityTest (Helpers)

- (TestEntity)givenTestEntityWithJSObject:(JSObject)jsObject
{
  var definition = [self givenEntityDefinitionForJSObject:jsObject];
  return [self givenTestEntityWithJSObject:jsObject andDefinition:definition];
}

- (TestEntity)givenTestEntityWithJSObject:(JSObject)jsObject andDefinition:(CPDictionary)definition
{
  var sessionMock = [[SessionMock alloc] init];
  [sessionMock stubDefinitionForClass:TestEntity returning:definition];

  return [[TestEntity alloc] initWithJSObject:jsObject session:sessionMock];
}

- (CPDictionary)givenEntityDefinitionForJSObject:(JSObject)jsObject
{
  var definition = [CPMutableDictionary dictionary];
  for(var propertyName in jsObject)
  {
    if ([propertyName hasSuffix:@"_cryson_id"] || [propertyName hasSuffix:@"_cryson_ids"] || jsObject[propertyName] instanceof Object) {
      [definition setObject:TestEntity forKey:[self _attributeNameFromRawAttributeName:propertyName]];
    } else {
      [definition setObject:"String" forKey:propertyName];
    }
  }
  return definition;
}

- (CPString)_attributeNameFromRawAttributeName:(CPString)rawAttributeName
{
  return [[[rawAttributeName stringByReplacingOccurrencesOfString:"_cryson_ids" withString:""]
                               stringByReplacingOccurrencesOfString:"_cryson_id" withString:""]
                                 stringByReplacingOccurrencesOfString:"_cryson_usertype" withString:""];
}

@end
