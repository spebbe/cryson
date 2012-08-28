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

@import "CrysonSession.j"
@import "CrysonDefinitionRepository.j"

@implementation CrysonSessionFactory : CPObject
{
  CPString baseUrl;
}

- (id)initWithBaseUrl:(CPString)aBaseUrl
{
  self = [super init];
  if (self) {
    baseUrl = aBaseUrl;
    [[CrysonDefinitionRepository sharedInstance] setBaseUrl:baseUrl];
    [[CrysonDefinitionRepository sharedInstance] fetchDefinitions];
  }
  return self;
}

- (CrysonSession)createSessionWithDelegate:(id)delegate
{
  return [[CrysonSession alloc] initWithBaseUrl:baseUrl andDelegate:delegate];
}

@end
