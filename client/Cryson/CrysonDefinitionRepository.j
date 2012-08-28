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

var sharedInstance = nil;

@implementation CrysonDefinitionRepository : CPObject
{
  CPString baseUrl @accessors;
  CPMutableDictionary definitions;
}

+ (CrysonDefinitionRepository)sharedInstance
{
  if (sharedInstance == nil) {
    sharedInstance = [[CrysonDefinitionRepository alloc] init];
  }
  return sharedInstance;
}

- (id)init
{
  self = [super init];
  if (self) {
    definitions = [[CPMutableDictionary alloc] init];
  }
  return self;
}

- (void)fetchDefinitions
{
  [RemoteService get:baseUrl + "/definitions"
            delegate:self
           onSuccess:@selector(foundDefinitions:)
             onError:@selector(failedToFindDefinitions:)
             context:nil];
}

- (CPDictionary)findDefinitionForClass:(CLASS)crysonEntityClass
{
  var definition = [definitions objectForKey:crysonEntityClass];
  if (definition == nil) {
    definition = [self _fetchDefinitionForClass:crysonEntityClass];
    [definitions setObject:definition forKey:crysonEntityClass];
  }
  return definition;
}

@end

@implementation CrysonDefinitionRepository (AsyncCallbacks)

- (void)foundDefinitions:(JSObject)rawDefinitions
{
  for(var className in rawDefinitions) {
    [definitions setObject:[self _definitionFromRawDefinition:rawDefinitions[className]] forKey:className];
  }
}

- (void)failedToFindDefinitions:(CPString)errorString
{
  console.log(errorString); // TODO: boohoo... what to do?
}

@end

@implementation CrysonDefinitionRepository (Private)


- (CPDictionary)_fetchDefinitionForClass:(CLASS)crysonEntityClass
{
  var rawDefinition = [RequestHelper syncGet:baseUrl + "/definition/" + crysonEntityClass.name];
  return [self _definitionFromRawDefinition:rawDefinition];
}

- (CPDictionary)_definitionFromRawDefinition:(JSObject)rawDefinition
{
  var definition = [[CPMutableDictionary alloc] init];
  for(var field in rawDefinition) {
    [definition setObject:[self _classFromString:(rawDefinition[field])] forKey:field];
  }
  return definition;
}

- (CLASS)_classFromString:(CPString)className
{
  var klazz = CPClassFromString(className);
  if (klazz) {
    return klazz;
  } else {
    return className;
  }
}

@end
