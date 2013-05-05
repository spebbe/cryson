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

@implementation TestEntity : CrysonEntity
{
}

@end


@implementation CrysonSessionTest : OJTestCase
{
}

- (void)testFindByClassAndId
{
  var delegateMock = moq();
  var delegateWasCalled = NO;
  [delegateMock selector:@selector(crysonSession:found:byClass:) callback:function(args) {
      [OJAssert assert:1 equals:[args[1] id]];
      [OJAssert assert:TestEntity equals:args[2]];
      delegateWasCalled = YES;
    }];

  var session = [self givenCrysonSessionWithRemoteEntity:{"id":1, "name":"test", "crysonEntityClass":"TestEntity"} andDelegate:delegateMock];
  [session findByClass:TestEntity andId:1 fetch:nil];

  [OJAssert assertTrue:delegateWasCalled];
}

- (void)testFindAllByClass
{
  var delegateMock = moq();
  var delegateWasCalled = NO;
  [delegateMock selector:@selector(crysonSession:foundAll:byClass:) callback:function(args) {
      [OJAssert assert:1 equals:[args[1] count]];
      [OJAssert assert:1 equals:[args[1][0] id]];
      [OJAssert assert:TestEntity equals:args[2]];
      delegateWasCalled = YES;
    }];

  var session = [self givenCrysonSessionWithRemoteEntity:{"id":1, "name":"test", "crysonEntityClass":"TestEntity"} andDelegate:delegateMock];
  [session findAllByClass:TestEntity fetch:nil];

  [OJAssert assertTrue:delegateWasCalled];
}

- (void)testPersistAndDeleteEntity
{
  var session = [self givenCrysonSession];
  var newEntity = [self givenTestEntity];

  [OJAssert assertFalse:[session dirty]];
  [session persist:newEntity];
  [OJAssert assertTrue:[session dirty]];
  [session delete:newEntity];
  [OJAssert assertFalse:[session dirty]];
}

- (void)testUnclassifiedErrorMessage
{
  var session = [self givenCrysonSession];
  var delegateMock = moq();
  var error = nil;
  [delegateMock selector:@selector(crysonSession:commitFailedWithError:) callback:function(args) {
      error = args[1];
    }];
                  
  [session commitFailed:@"some barf from the server"
             statusCode:500
                context:[CrysonSessionContext contextWithDelegate:delegateMock]];
  
  [OJAssert assert:@"Unclassified error" equals:[error message]];
  [OJAssert assert:500 equals:[error statusCode]];
  [OJAssert assert:0 equals:[[error validationFailures] count]];
}

- (void)testValidationErrorMessage
{
  var session = [self givenCrysonSession];
  [session setCrysonDefinitionRepository:[self givenCrysonDefinitionRepository]];
  var delegateMock = moq();
  var error = nil;
  [delegateMock selector:@selector(crysonSession:commitFailedWithError:) callback:function(args) {
      error = args[1];
    }];
  var testEntity = [self givenTestEntity];
  [testEntity setId:1];
  [testEntity setName:@"test"];
  [session attach:testEntity];
                  
  [session commitFailed:JSON.stringify({"message":"Some validation failed", "validationFailures":[{"message":"validation failed",entityClass:"TestEntity",entityId:1,keyPath:"name"}]})
             statusCode:403
                context:[CrysonSessionContext contextWithDelegate:delegateMock]];
  
  [OJAssert assert:@"Some validation failed" equals:[error message]];
  [OJAssert assert:403 equals:[error statusCode]];
  [OJAssert assert:1 equals:[[error validationFailures] count]];
  var validationFailure = [[error validationFailures] objectAtIndex:0];
  [OJAssert assert:testEntity equals:[validationFailure entity]];
  [OJAssert assert:@"name" equals:[validationFailure keyPath]];
  [OJAssert assert:@"test" equals:[validationFailure value]];
  [OJAssert assert:@"validation failed" equals:[validationFailure message]];
}

- (void)testPersistEntityThatIsModifiedClientSideDuringCommitOperation
{
  var testEntity = [self givenTestEntity];
  [testEntity setName:@"committed"];

  var remoteService = moq();
  var postCallback = function(args /*object, requestUrl, delegate, onSuccess, onError, context*/) {
    [testEntity setName:@"updatedDuringCommitOperation"];
    var commitResult = {"replacedTemporaryIds":{"-1": 14}, "persistedEntities":[{"crysonEntityClass":"TestEntity","id":14,"name":"committed"}], "updatedEntities":[], "deletedEntities":[]};
    [args[2] performSelector:args[3] withObject:commitResult withObject:args[5]];
  };
  [remoteService selector:@selector(post:to:delegate:onSuccess:onError:context:) callback:postCallback];
  var delegateMock = moq();
  var session = [self givenCrysonSessionWithRemoteService:remoteService andDelegate:delegateMock];
  [session setCrysonDefinitionRepository:[self givenCrysonDefinitionRepository]];
  [session persist:testEntity];
  [OJAssert assert:-1 equals:[testEntity id]];
  [OJAssert assert:@"committed" equals:[testEntity name]];  
  [OJAssert assertTrue:[testEntity dirty]];
  [session commit];
  [OJAssert assert:14 equals:[testEntity id]];
  [OJAssert assert:@"updatedDuringCommitOperation" equals:[testEntity name]];
  [OJAssert assertTrue:[testEntity dirty]];
}

- (void)testPersistEntityThatIsModifiedServerSideDuringCommitOperation
{
  var testEntity = [self givenTestEntity];
  [testEntity setName:@"committed"];

  var remoteService = moq();
  var postCallback = function(args /*object, requestUrl, delegate, onSuccess, onError, context*/) {
    var commitResult = {"replacedTemporaryIds":{"-1": 14}, "persistedEntities":[{"crysonEntityClass":"TestEntity","id":14,"name":"updatedOnServer"}], "updatedEntities":[], "deletedEntities":[]};
    [args[2] performSelector:args[3] withObject:commitResult withObject:args[5]];
  };
  [remoteService selector:@selector(post:to:delegate:onSuccess:onError:context:) callback:postCallback];
  var delegateMock = moq();
  var session = [self givenCrysonSessionWithRemoteService:remoteService andDelegate:delegateMock];
  [session setCrysonDefinitionRepository:[self givenCrysonDefinitionRepository]];
  [session persist:testEntity];
  [OJAssert assert:-1 equals:[testEntity id]];
  [OJAssert assert:@"committed" equals:[testEntity name]];  
  [OJAssert assertTrue:[testEntity dirty]];
  [session commit];
  [OJAssert assert:14 equals:[testEntity id]];
  [OJAssert assert:@"updatedOnServer" equals:[testEntity name]];
  [OJAssert assertFalse:[testEntity dirty]];
}

- (void)testDirtyCallbackWhenPersistingEntity
{
  var delegateWasCalled = NO;
  var delegateMock = moq();
  [delegateMock selector:@selector(crysonSession:dirtyDidChangeForEntity:) callback:function(args /* crysonSession, dirtyObject */) {
      [OJAssert assert:testEntity equals:args[1]];
      [OJAssert assertTrue:[args[1] dirty]];
      delegateWasCalled = YES;
    }];

  var testSession = [self givenCrysonSessionWithRemoteService:moq() andDelegate:delegateMock];
  var testEntity = [self givenTestEntity];
  [OJAssert assertFalse:[testEntity dirty]];
  [OJAssert assertFalse:[testSession dirty]];
  [OJAssert assertFalse:delegateWasCalled];
  [testSession persist:testEntity];
  [OJAssert assertTrue:delegateWasCalled];
  [OJAssert assertTrue:[testEntity dirty]];
  [OJAssert assertTrue:[testSession dirty]];
}

- (void)testDirtyCallbackWhenDeletingEntity
{
  var delegateWasCalled = NO;
  var delegateMock = moq();
  [delegateMock selector:@selector(crysonSession:dirtyDidChangeForEntity:) callback:function(args /* crysonSession, dirtyObject */) {
      [OJAssert assert:testEntity equals:args[1]];
      [OJAssert assertTrue:[args[1] dirty]];
      delegateWasCalled = YES;
    }];

  var testSession = [self givenCrysonSessionWithRemoteService:moq() andDelegate:delegateMock];
  var testEntity = [self givenTestEntity];
  [testEntity setId:100];
  [testEntity virginize];
  [testSession attach:testEntity];
  [OJAssert assertFalse:[testEntity dirty]];
  [OJAssert assertFalse:[testSession dirty]];
  [OJAssert assertFalse:delegateWasCalled];
  [testSession delete:testEntity];
  [OJAssert assertTrue:delegateWasCalled];
  [OJAssert assertTrue:[testEntity dirty]];
  [OJAssert assertTrue:[testSession dirty]];
}

@end

@implementation CrysonSessionTest (Helpers)

- (TestEntity)givenTestEntity
{
  var testEntity = [[TestEntity alloc] init];
  testEntity.cachedDefinition = [self givenDefinitionMock];
  return testEntity;
}

- (CrysonSession)givenCrysonSession
{
  return [self givenCrysonSessionWithRemoteService:moq() andDelegate:moq()];
}

- (CrysonSession)givenCrysonSessionWithRemoteEntity:(JSObject)jsObject andDelegate:(id)aDelegate
{
  var remoteService = moq();
  var getCallback = function(args /*requestUrl, delegate, onSuccess, onError, context*/) {
    if (args[2] == @selector(findByClassAndIdSucceeded:context:)) {
      [args[1] performSelector:args[2] withObject:jsObject withObject:args[4]];
    } else if (args[2] == @selector(findAllByClassSucceeded:context:)) {
      [args[1] performSelector:args[2] withObject:[jsObject] withObject:args[4]];
    }
  };
  [remoteService selector:@selector(get:delegate:onSuccess:onError:context:) callback:getCallback];

  return [self givenCrysonSessionWithRemoteService:remoteService andDelegate:aDelegate];
}

- (CrysonSession)givenCrysonSessionWithRemoteService:(RemoteService)aRemoteService andDelegate:(id)aDelegate
{
  var crysonSession = [[CrysonSession alloc] initWithBaseUrl:"test://test" andDelegate:aDelegate];
  [crysonSession setRemoteService:aRemoteService];
  [crysonSession setCrysonDefinitionRepository:[self givenCrysonDefinitionRepository]];
  return crysonSession;
}

- (CrysonDefinitionRepository)givenCrysonDefinitionRepository
{
  var crysonDefinitionRepositoryMock = moq();
  [crysonDefinitionRepositoryMock selector:@selector(findDefinitionForClass:) returns:[self givenDefinitionMock]];
  return crysonDefinitionRepositoryMock;
}

- (CPDictionary)givenDefinitionMock
{
  var definitionMock = moq();
  [definitionMock selector:@selector(objectForKey:) returns:@"Long" arguments:[@"id"]];
  [definitionMock selector:@selector(objectForKey:) returns:@"String"];
  [definitionMock selector:@selector(allKeys) returns:[@"id", @"name"]];
  return definitionMock;
}

@end
