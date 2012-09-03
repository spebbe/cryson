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

@implementation CrysonSessionCache : CPObject
{
  CPMutableDictionary cachedEntities;
}

- (id)init
{
  self = [super init];
  if (self) {
    cachedEntities = [[CPMutableDictionary alloc] init];
  }
  return self;
}

- (void)addEntity:(CrysonEntity)entity
{
  var hashKeys = [self _hashKeysForEntity:entity],
      hashKeysEnumerator = [hashKeys objectEnumerator],
      currentHashKey = nil;
  while(currentHashKey = [hashKeysEnumerator nextObject]) {
    [cachedEntities setObject:entity forKey:currentHashKey];
  }
}

- (void)removeEntity:(CrysonEntity)entity
{
  var hashKeys = [self _hashKeysForEntity:entity],
      hashKeysEnumerator = [hashKeys objectEnumerator],
      currentHashKey = nil;
  while(currentHashKey = [hashKeysEnumerator nextObject]) {
    [cachedEntities removeObjectForKey:currentHashKey];
  }
}

- (CrysonEntity)findByClass:(CLASS)klazz andId:(int)id
{
  var hashKeys = [self _hashKeysForClass:klazz andId:id],
      hashKeysEnumerator = [hashKeys objectEnumerator],
      currentHashKey = nil;
  while(currentHashKey = [hashKeysEnumerator nextObject]) {
    var entity = [cachedEntities objectForKey:currentHashKey];
    if (entity) {
      return entity;
    }
  }
  return nil;
}

- (CPEnumerator)entityEnumerator
{
  return [_.uniq([cachedEntities allValues]) objectEnumerator];
}

- (CPArray)_hashKeysForEntity:(CrysonEntity)entity
{
  return [self _hashKeysForClass:[entity class] andId:[entity id]];
}

- (CPArray)_hashKeysForClass:(CLASS)klazz andId:(int)id
{
  var hashKeys = [],
      currentKlazz = klazz;
  while(currentKlazz != CrysonEntity) {
    [hashKeys addObject:(currentKlazz.name + "_" + id)];
    currentKlazz = [currentKlazz superclass];
  }
  return hashKeys;
}

@end
