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

@implementation CrysonSessionContext : CPObject
{
  id delegate @accessors;
  CLASS entityClass @accessors;
  CPNumber entityId @accessors;
  CPNumber statusCode @accessors;
  CrysonEntity example @accessors;
  CPArray updatedEntities @accessors;
  CPArray deletedEntities @accessors;
  CPArray persistedEntities @accessors;
  CPString errorMessage @accessors;
  CrysonEntity entityToRefresh @accessors;
  CPString namedQuery @accessors;
  CPDictionary persistedEntitiesSnapshot @accessors;
  CPDictionary updatedEntitiesSnapshot @accessors;
}

+ (CrysonSessionContext)contextWithDelegate:(id)aDelegate andEntityClass:(CLASS)anEntityClass
{
  var context = [[CrysonSessionContext alloc] init];
  [context setDelegate:aDelegate];
  [context setEntityClass:anEntityClass];
  return context;
}

+ (CrysonSessionContext)contextWithDelegate:(id)aDelegate
{
  return [CrysonSessionContext contextWithDelegate:aDelegate andEntityClass:nil];
}

@end
