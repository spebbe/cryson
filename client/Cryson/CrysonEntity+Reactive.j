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

@import <RCFoundation/RCFoundation.j>
@import "CrysonEntity.j"

@implementation CrysonEntity (Reactive)

+ (void)initializeDefinition:(CPDictionary)aDefinition
{
  var enumerator = [aDefinition keyEnumerator],
      attributeName;

  while ((attributeName = [enumerator nextObject]) !== nil) {
    (function() {
      var attribute = [aDefinition objectForKey:attributeName];
      if ([attribute isKindOfClass:CrysonEntity]) {
        var selector = sel_getUid(attributeName + "RC"),
            attributeSel = sel_getUid(attributeName + "");
        if (![self respondsToSelector:selector]) {
          class_addMethod(self, selector, function(s, _cmd) {
            return [RCProperty constant:[s performSelector:attributeSel]];
          }, "");
        }
      }
    })();
  }
}

@end
