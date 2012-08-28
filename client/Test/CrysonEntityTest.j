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
if (typeof(_) == 'undefined') {
  _ = exports._;
}
@import <OJMoq/OJMoq.j>

@implementation TestEntity : CrysonEntity
{
}

@end

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

  [[entity session] selector:@selector(findSyncByClass:andId:fetch:) returns:innerEntity arguments:[TestEntity, 2, nil]];

  [OJAssert assert:@"outer" equals:[entity name]];
  [OJAssert assert:@"inner" equals:[[entity something] name]];
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

  [[entity session] selector:@selector(findSyncByClass:andIds:fetch:) returns:[innerEntity] arguments:[TestEntity, [2], nil]];

  [OJAssert assert:@"outer" equals:[entity name]];
  [OJAssert assert:@"inner" equals:[[[entity something] objectAtIndex:0] name]];
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

@implementation CrysonEntityTest (Helpers)

- (TestEntity)givenTestEntityWithJSObject:(JSObject)jsObject
{
  var definition = [self givenEntityDefinitionForJSObject:jsObject];
  return [self givenTestEntityWithJSObject:jsObject andDefinition:definition];
}

- (TestEntity)givenTestEntityWithJSObject:(JSObject)jsObject andDefinition:(CPDictionary)definition
{
  var sessionMock = moq();
  [sessionMock selector:@selector(findDefinitionForClass:) returns:definition arguments:[TestEntity]];
  [sessionMock selector:@selector(findCachedByClass:andId:) returns:nil];
  
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
