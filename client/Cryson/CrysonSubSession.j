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

@import "CrysonEntity.j"
@import "CrysonMutableEntitySet.j"
@import "CrysonSession.j"

/*!
  @class CrysonSubSession

  CrysonSubSession instances encapsulate a subset of entities within a parent CrysonSession.
  Entities attached to a sub session can be committed and rolled back independent of other entities in the parent session.
*/
@implementation CrysonSubSession : CPObject
{
  CrysonSession                crysonSession;
  CrysonMutableEntitySet       trackedEntities;
}

- (id)initWithSession:(CrysonSession)aCrysonSession
{
  self = [super init];
  if (self) {
    crysonSession = aCrysonSession;
    trackedEntities = [[CrysonMutableEntitySet alloc] init];
  }
  return self;
}

/*!
  @see CrysonSession#commitWithDelegate:
*/
- (void)commitWithDelegate:(id)aDelegate
{
  [crysonSession commitEntities:[trackedEntities allObjects] delegate:aDelegate];
}

/*!
  @see CrysonSession#rollback
*/
- (void)rollback
{
  [crysonSession rollbackEntities:[trackedEntities allObjects]];
}

/*!
  @see CrysonSession#persist:
*/
- (void)persist:(CrysonEntity)anEntity
{
  [self _trackEntities:anEntity];
  [crysonSession persist:anEntity];
}

/*!
  @see CrysonSession#attach:
*/
- (void)attach:(CrysonEntity)anEntity
{
  [self _trackEntities:anEntity];
  if (anEntity instanceof Array) {
    for(var ix = 0;ix < [anEntity count];ix++) {
      [crysonSession attach:[anEntity objectAtIndex:ix]];
    }
  } else {
    [crysonSession attach:anEntity];
  }
}

/*!
  @see CrysonSession#delete:
*/
- (void)delete:(CrysonEntity)anEntity
{
  [self _trackEntities:anEntity];
  [crysonSession delete:anEntity];
}

- (void)_trackEntities:(id)entities
{
  if (entities instanceof Array) {
    for(var ix = 0;ix < [entities count];ix++) {
      [trackedEntities addObject:[entities objectAtIndex:ix]];
    }
  } else if (entities != nil) {
    [trackedEntities addObject:entities];
  }
}

@end
