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

function compareNumbers(a,b){
  return a-b;
}

var ArrayKVCOperationRegexp = /^(countOf(.+))|(objectIn(.+)AtIndex:)|(insertObject:in(.+)AtIndex:)|(removeObjectFrom(.+)AtIndex:)$/;
var KVCValidationRegexp = /^(validate(.+):)$/;
var NumberTypes = [CrysonMutableEntitySet setWithArray:["Long", "long", "Integer", "int", "Float", "float", "Double", "double"]];
var NullableTypes = [CrysonMutableEntitySet setWithArray:["Long", "Integer", "Float", "Double", "Date", "String"]];

// TODO: Time for a cleanup again...:
//       Extract forwardInvocation stuff from entity and mapwrapper into common base class.
//       merge handling of association and userTypes if possible

/*!
  @class CrysonEntity

  CrysonEntity is the base class which all entity classes must extend. CrysonEntity subclasses dynamically create accessor methods for any attributes and associations defined on the corresponding entity. Changes to attributes and associations are automatically tracked and communicated to the associated CrysonSession. Associated entities are automatically connected and are lazily fetched from a Cryson server if not already in the session cache when first accessed. A few restrictions/assumptions apply to all concrete subclasses of CrysonEntity:
- The Objective-J class name _must_ exactly match the corresponding Hibernate mapped entity class on the Cryson server.
- The primary key field _must_ be of type Long and _must_ be named 'id'.
- Persistent primary keys _must_ be positive long integers.

  Note: All persistent attributes of a CrysonEntity instance are KVC, KVO and KVB compliant and will fire changes as they are updated.
        To-many associations are however implemented usng normal CPArray instances and therefore the normal rules regarding
        KVC/KVO behaviour for CPArray apply. For example, [entity insertObject:x in<Nameofassociation>AtIndex:y] will trigger KVO, whereas
        [[entity <nameOfAssociation>] insertObject:x atIndex:y] will not.
*/
@implementation CrysonEntity : CPObject
{
  CrysonSession session @accessors;
  JSObject crysonObject;
  JSObject crysonAssociations;
  JSObject crysonUserTypes;
  CPDictionary cachedDefinition;
  CPString virginJSObject;
  CrysonEntityAsyncProxy crysonEntityAsyncProxy;
}

/*!
  Non-persistent instances should always be instantiated using the no-args constructor.
  Returned instances are not associated with any session and do not have a valid identifier.
  An non-persistent instance becomes a persistent entity by being passed as argument to
  CrysonSession#persist:, which will assign an identifier and attach the entity to the
  persisting session. Use CrysonEntity#initWithSession: to create new persistent instances
  for which a session is already available.

  @see CrysonSession#persist:
  @see CrysonEntity#initWithSession:
*/
- (id)init
{
  self = [super init];
  if (self) {
    session = nil;
    cachedDefinition = [self definition];
    crysonObject = {};
    crysonObject["id"] = nil;
    crysonAssociations = {};
    crysonUserTypes = [self createEmptyUserTypes];
    [self virginize];
  }
  return self;
}

/*!
  Initializes and associates a new entity with a session, scheduling the new entity for
  persistence on the next commit operation.

  @see CrysonEntity#init
  @see CrysonSession#persist:
*/
- (id)initWithSession:(CrysonSession)aSession
{
  self = [self init];
  if (self) {
    [aSession persist:self];
  }
  return self;
}

- (JSObject)createEmptyUserTypes
{
  var result = {};
  var attributeEnumerator = [cachedDefinition keyEnumerator];
  var attribute;
  while((attribute = [attributeEnumerator nextObject]) != nil) {
    var attributeClass = [cachedDefinition objectForKey:attribute];
    if ((typeof(attributeClass) ==  "string") && [attributeClass hasPrefix:"UserType_"]) {
      if (attributeClass == "UserType_Map") {
        result[attribute] = [[CrysonMapWrapper alloc] initWithParentEntity:self parentAttributeName:attribute andAttributes:{}];
      } else {
        [CPException raise:"CrysonException" reason:("Unknown UserType class: " + attributeClass)];
      }
    }
  }
  return result;
}

- (CPDictionary)definition
{
  if (session) {
    return [session findDefinitionForClass:[self class]];
  } else {
    // TODO: Ugly workaround for non-persistent entities.
    return [[CrysonDefinitionRepository sharedInstance] findDefinitionForClass:[self class]];
  }
}

- (id)initWithJSObject:(JSObject)jsonObject session:(CrysonSession)aSession
{
  self = [super init];
  if (self)
  {
    session = aSession;
    [self setAttributesFromJSObject:jsonObject];
    [self virginize];
  }
  return self;
}

- (void)setAttributesFromJSObject:(JSObject)jsonObject
{
  crysonAssociations = {};
  cachedDefinition = [self definition];
  [self populateCrysonObjectFromJSONObject:jsonObject];
}

- (JSObject)populateCrysonObjectFromJSONObject:(JSObject)jsonObject
{
  crysonObject = {};
  crysonUserTypes = {};
  for(var attributeName in jsonObject) {
    if ([self isEmbeddedAttribute:attributeName jsonObject:jsonObject]) {
      var embeddedAttribute = jsonObject[attributeName];
      if (embeddedAttribute instanceof Array) {
        var embeddedIds = [];
        var embeddedEntities = [];
        for(var ix = 0;ix < embeddedAttribute.length;ix++) {
          embeddedEntities[ix] = [self resolveAssociation:embeddedAttribute[ix] withName:attributeName];
          embeddedIds[ix] = embeddedAttribute[ix].id;
        }
        crysonAssociations[attributeName] = embeddedEntities;
        crysonObject[[self idsAttributeNameFromAttributeName:attributeName]] = embeddedIds;
      } else {
        crysonAssociations[attributeName] = [self resolveAssociation:embeddedAttribute withName:attributeName];
        crysonObject[[self idAttributeNameFromAttributeName:attributeName]] = embeddedAttribute.id;
      }
    } else if ([self isUserTypeAttributeName:attributeName]) {
      var actualAttributeName = [self attributeName:attributeName];
      var userTypeClass = [cachedDefinition objectForKey:actualAttributeName];
      if (userTypeClass == "UserType_Map") {
        crysonUserTypes[actualAttributeName] = [[CrysonMapWrapper alloc] initWithParentEntity:self parentAttributeName:actualAttributeName andAttributes:jsonObject[attributeName]];
      } else {
        [CPException raise:"CrysonException" reason:("Unknown UserType class: " + userTypeClass)];
      }
    } else {
      crysonObject[attributeName] = jsonObject[attributeName];
    }
  }
}

- (BOOL)isEmbeddedAttribute:(CPString)attributeName jsonObject:(JSObject)jsonObject
{
  if ([self isIDAttributeName:attributeName] || [self isUserTypeAttributeName:attributeName]) {
    return false;
  }
  return (jsonObject[attributeName] instanceof Object || jsonObject[attributeName] instanceof Array);
}

- (BOOL)isUserTypeAttributeName:(CPString)attributeName
{
  return [attributeName hasSuffix:"_cryson_usertype"];
}

- (CPString)userTypeAttributeNameFromAttributeName:(CPString)attributeName
{
  return attributeName + "_cryson_usertype";
}

- (BOOL)isIDAttributeName:(CPString)attributeName
{
  return ([attributeName hasSuffix:"_cryson_id"] || [attributeName hasSuffix:"_cryson_ids"]);
}

- (CPString)idAttributeNameFromAttributeName:(CPString)attributeName
{
  return attributeName + "_cryson_id";
}

- (CPString)idsAttributeNameFromAttributeName:(CPString)attributeName
{
  return attributeName + "_cryson_ids";
}

- (BOOL)isUserTypeAttribute:(CPString)attributeName
{
  var attributeClassName = [cachedDefinition objectForKey:attributeName];
  return attributeClassName && [attributeName hasPrefix:"UserType_"];
}

- (CrysonEntity)resolveAssociation:(JSObject)jsonObject withName:(CPString)associationName
{
  var associationClass = [cachedDefinition objectForKey:associationName];
  if (typeof(associationClass) ==  "string") {
    [CPException raise:"CrysonException" reason:("Unknown CrysonEntity: " + associationClass)];
  }
  var resolvedAssociation = [session findCachedByClass:associationClass andId:jsonObject.id];
  if (!resolvedAssociation) {
    var actualAssociationClass = CPClassFromString(jsonObject.crysonEntityClass);
    resolvedAssociation = [[actualAssociationClass alloc] initWithJSObject:jsonObject session:session];
    [session attach:resolvedAssociation];
  } else {
    [resolvedAssociation populateCrysonObjectFromJSONObject:jsonObject];
  }
  return resolvedAssociation;
}

- (id)initWithCoder:(CPCoder)coder
{
  return [self initWithJSObject:[coder decodeObjectForKey:"jsObject"] session:nil];
}

- (void)encodeWithCoder:(CPCoder)coder
{
  [coder encodeObject:[self toJSObject] forKey:"jsObject"];
}

- (BOOL)respondsToSelector:(SEL)aSelector
{
  if ([super respondsToSelector:aSelector]) {
    return YES;
  }

  var selectorString = CPStringFromSelector(aSelector),
      matches = selectorString.match(ArrayKVCOperationRegexp) || selectorString.match(KVCValidationRegexp);
  if (matches) {
    var key = _.last(_.compact(matches));
    var attributeName = key.charAt(0).toLowerCase() + key.slice(1);
    if ([cachedDefinition objectForKey:attributeName]) {
      return YES;
    }
  }

  return NO;
}

- (CPMethodSignature)methodSignatureForSelector:(SEL)selector {
    return YES;
}

- (void)forwardInvocation:(CPInvocation)anInvocation {
  var aSelector = [anInvocation selector],
      selectorString = CPStringFromSelector(aSelector),
      matches;
  if (selectorString.indexOf('set') == 0 && selectorString.indexOf(':')!=-1) {
    var unSettedSelectorString = selectorString.replace(/^set/, '').replace(':', '');
    var attributeName = unSettedSelectorString.charAt(0).toLowerCase() + unSettedSelectorString.slice(1);
    [anInvocation setReturnValue:[self _setAttribute:attributeName toValue:[anInvocation argumentAtIndex:2]]];
  } else if (matches = selectorString.match(KVCValidationRegexp)) {
    var attributeName = matches[2].charAt(0).toLowerCase() + matches[2].slice(1);
    [anInvocation setReturnValue:[self _validateValue:[anInvocation argumentAtIndex:2] forAttribute:attributeName]];
  } else {
    var matches = selectorString.match(ArrayKVCOperationRegexp);
    if (matches) {
      var key = _.last(_.compact(matches));
      var attributeName = key.charAt(0).toLowerCase() + key.slice(1);
      [self forwardArrayKVCInvocation:anInvocation forAttribute:attributeName];
    } else {
      [anInvocation setReturnValue:[self _getAttribute:selectorString]];
    }
  }
}

- (void)forwardArrayKVCInvocation:(CPInvocation)anInvocation forAttribute:(CPString)anAttribute
{
  var selectorString = CPStringFromSelector([anInvocation selector]);
  var firstChar = selectorString.charAt(0);
  var attributeArray = [self valueForKey:anAttribute];
  if (attributeArray == nil) {
    attributeArray = [];
    [self setValue:attributeArray forKey:anAttribute];
  }
  if (firstChar == 'c') {
    [anInvocation setReturnValue:[attributeArray count]];
  } else if (firstChar == 'o') {
    [anInvocation setReturnValue:[attributeArray objectAtIndex:[anInvocation argumentAtIndex:2]]];
  } else if (firstChar == 'i') {
    [self willChangeValueForKey:"dirty"];
    [self willChangeValueForKey:anAttribute];
    [attributeArray insertObject:[anInvocation argumentAtIndex:2] atIndex:[anInvocation argumentAtIndex:3]];
    [self didChangeValueForKey:anAttribute];
    [self didChangeValueForKey:"dirty"];
  } else if (firstChar == 'r') {
    [self willChangeValueForKey:"dirty"];
    [self willChangeValueForKey:anAttribute];
    [attributeArray removeObjectAtIndex:[anInvocation argumentAtIndex:2]];
    [self didChangeValueForKey:anAttribute];
    [self didChangeValueForKey:"dirty"];
  } else {
    [self doesNotRecognizeSelector:[anInvocation selector]];
  }
}

- (CPValidationResult)_validateValue:(id)attributeValue forAttribute:(CPString)attributeName
{
  var attributeType = [cachedDefinition objectForKey:attributeName];

  if ((attributeValue == null || (attributeValue === "" && attributeType != "String")) &&
      [self _attributeIsNullable:attributeType]) {
    return [CPValidationResult validationResultWithValue:null valid:YES];
  }

  if ([NumberTypes containsObject:attributeType]) {
    var coercedNumber = +attributeValue;
    if (_.isNaN(coercedNumber)) {
      return [CPValidationResult validationResultWithValue:crysonObject[attributeName] valid:YES];
    } else {
      return [CPValidationResult validationResultWithValue:coercedNumber valid:YES];
    }
  } else if (attributeType == "Date") {
    if (attributeValue.isa == CPDate) {
      return [CPValidationResult validationResultWithValue:attributeValue valid:YES];
    }

    try {
      return [CPValidationResult validationResultWithValue:[[CPDate alloc] initWithString:attributeValue] valid:YES];
    } catch (e) {
      var date = [self _reverseCoerceValue:attributeName];
      return [CPValidationResult validationResultWithValue:date valid:YES];
    }
  }

  return [CPValidationResult validationResultWithValue:attributeValue valid:YES];
}

- (BOOL)_attributeIsNullable:(CPString)anAttributeType
{
  return [NullableTypes containsObject:anAttributeType] || [anAttributeType hasPrefix:@"UserType_"];
}

- (void)setValue:(id)aValue forKey:(CPString)aKey
{
  var selector = CPSelectorFromString('set' + aKey.charAt(0).toUpperCase() + aKey.substr(1) + ':');
  if ([self respondsToSelector:selector]) {
    [self performSelector:selector withObject:aValue];
    return;
  }
  [self _setAttribute:aKey toValue:aValue];
}

- (id)valueForKey:(CPString)aKey
{
  var selector = CPSelectorFromString(aKey);
  if ([self respondsToSelector:selector]) {
    return [self performSelector:selector];
  }
  return [self _getAttribute:aKey];
}

- (id)_setAttribute:(CPString)attributeName toValue:(id)attributeValue
{
  [self willChangeValueForKey:"dirty"];
  [self willChangeValueForKey:attributeName];
  if ([cachedDefinition objectForKey:attributeName] instanceof Object) {
    if ([self isUserTypeAttribute:attributeName]) {
      crysonUserTypes[attributeName] = attributeValue;
    } else {
      crysonAssociations[attributeName] = attributeValue;
    }
  } else {
    crysonObject[attributeName] = [self coerceValue:attributeValue forAttribute:attributeName];
  }
  [self didChangeValueForKey:attributeName];
  [self didChangeValueForKey:"dirty"];
  return self;
}

- (id)coerceValue:(id)attributeValue forAttribute:(CPString)attributeName
{
  if (_.isNull(attributeValue)) {
    return nil;
  }
  var attributeType = [cachedDefinition objectForKey:attributeName];
  if ([NumberTypes containsObject:attributeType]) {
   var coercedNumber = +attributeValue;
   if (_.isNaN(coercedNumber)) {
     var errorMessage = ("[" +[self className] + " " + attributeName + "]: " + attributeValue + " cannot be safely coerced into number");
     // [CPException raise:CPInvalidArgumentException reason:errorMessage];
     CPLog.warn("WARNING: " + errorMessage);
     return crysonObject[attributeName];
   } else {
     return coercedNumber;
   }
  } else if (attributeType == "String") {
    return "" + attributeValue;
  } else if (attributeType == "boolean") {
    if (_.isBoolean(attributeValue)) {
      return attributeValue;
    }
    if (!_.isNaN(+attributeValue)) {
      return +attributeValue != 0;
    }
    return attributeValue != "false";
  } else if (attributeType == "Date") {
    return attributeValue == null ? null : [attributeValue description];
  } else {
    // TODO: More coercions...
  }
  return attributeValue;
}

- (id)_getAttribute:(CPString)attributeName
{
  var foundAttribute = crysonUserTypes[attributeName];
  if (foundAttribute === undefined) {
    if (typeof([cachedDefinition objectForKey:attributeName]) == "string") {
      return [self _reverseCoerceValue:attributeName];
    } else {
      return [self materializeAssociation:attributeName];
    }
  }
  return foundAttribute;
}

- (id)_reverseCoerceValue:(CPString)attributeName
{
  var attributeType = [cachedDefinition objectForKey:attributeName],
      attributeValue = crysonObject[attributeName];

  if ([NumberTypes containsObject:attributeType]) {
    return attributeValue;
  } else if (attributeType == "Date") {
    return attributeValue == null ? null : [[CPDate alloc] initWithString:attributeValue];
  } else {
    return attributeValue;
  }
}

- (id)materializeAssociation:(CPString)associationName
{
  if (crysonAssociations[associationName] === undefined) {
    var associationClass = [cachedDefinition objectForKey:associationName];
    if (associationClass == nil) {
      [self doesNotRecognizeSelector:CPSelectorFromString(associationName)];
    }
    if (typeof(associationClass) ==  "string") {
      [CPException raise:"CrysonException" reason:("Unknown CrysonEntity: " + associationClass)];
    }

    var associationId = crysonObject[associationName + "_cryson_id"];
    if (associationId === undefined) {
      associationId = crysonObject[associationName + "_cryson_ids"];
    }

    if (associationId instanceof Array) {
      if ([associationId count] > 0) {
        if (crysonEntityAsyncProxy && [crysonEntityAsyncProxy withinAsyncOperation])  {
          return [crysonEntityAsyncProxy loadAssociation:associationName byClass:associationClass andIds:associationId];
        }
        crysonAssociations[associationName] = [session findSyncByClass:associationClass andIds:associationId fetch:nil];
      } else {
        crysonAssociations[associationName] = [];
      }
    } else {
      if (associationId) {
        if (crysonEntityAsyncProxy && [crysonEntityAsyncProxy withinAsyncOperation])  {
          return [crysonEntityAsyncProxy loadAssociation:associationName byClass:associationClass andId:associationId];
        }
        crysonAssociations[associationName] = [session findSyncByClass:associationClass andId:associationId fetch:nil];
      } else {
        crysonAssociations[associationName] = nil;
      }
    }
  }
  return crysonAssociations[associationName];
}

- (void)virginize
{
  [self willChangeValueForKey:"dirty"];

  [self resetVirgin];

  [self didChangeValueForKey:"dirty"];
}

- (void)resetVirgin
{
  // TODO: find cheaper way to do deep clone, maybe?
  virginJSObject = JSON.parse(JSON.stringify([self toJSObject]));
}

- (JSObject)toJSObject
{
  var result = {};

  for (var rawAttribute in crysonObject) {
    if (![self isIDAttributeName:rawAttribute] || !crysonAssociations[[self attributeName:rawAttribute]]) {
      if (crysonObject[rawAttribute] instanceof Array && [self isIDAttributeName:rawAttribute]) {
        result[rawAttribute] = crysonObject[rawAttribute].sort(compareNumbers);
      } else {
        result[rawAttribute] = crysonObject[rawAttribute];
      }
    }
  }

  for (var attributeName in crysonAssociations) {
    var associationValue = crysonAssociations[attributeName];
    if (associationValue instanceof Array) {
      var jsIdArray = [];
      for(var ix = 0;ix < associationValue.length;ix++) {
        jsIdArray[ix] = [associationValue[ix] id];
      }
      result[attributeName + "_cryson_ids"] = jsIdArray.sort(compareNumbers);
    } else {
      result[attributeName + "_cryson_id"] = associationValue ? [associationValue id] : null;
    }
  }

  for (var attributeName in crysonUserTypes) {
    result[attributeName + "_cryson_usertype"] = [crysonUserTypes[attributeName] toJSObject];
  }

  return result;
}

- (CPString)attributeName:(CPString)rawAttributeName
{
  return [[[rawAttributeName stringByReplacingOccurrencesOfString:"_cryson_ids" withString:""]
                               stringByReplacingOccurrencesOfString:"_cryson_id" withString:""]
                                 stringByReplacingOccurrencesOfString:"_cryson_usertype" withString:""];
}

/*!
  Return a boolean indicating whether the entity has been changed since it was last loaded/refreshed or committed.
  Recognized changes include modifications to direct attributes, including changes to embedded maps, as well as
  changed references to associated entities. In contrast, changes to attributes of associated entities do not affect
  the dirty state of an entity. Dirty entities are affected by commit and rollback operations on their associated sessions.

  Note: The dirty attribute is KVO and KVB compliant and will fire changes as the entity becomes or stops being dirty.

  @see CrysonSession#commit
  @see CrysonSession#rollback
*/
- (BOOL)dirty
{
  return !_.isEqual([self toJSObject], virginJSObject);
}

/*!
  Revert any changes to direct attributes, including changes to embedded maps, as well as changed references to associated entities,
  made since the entity was last loaded/refreshed or committed.
  A rollback is a purely client side operation and does not involve server side communication.

  @see CrysonSession#rollback
*/
- (void)revert
{
  if ([self dirty]) {
    [self refreshWithJSObject:virginJSObject];
  }
}

- (void)refreshWithJSObject:(JSObject)newObject
{
  for(var attributeName in newObject) {
    [self willChangeValueForKey:[self attributeName:attributeName]];
  }
  [self setAttributesFromJSObject:newObject];
  for(var attributeName in newObject) {
    [self didChangeValueForKey:[self attributeName:attributeName]];
  }
  [self resetVirgin];
}

- (void)willChangeValueForKey:(CPString)aKey
{
  [super willChangeValueForKey:aKey];
  if (crysonEntityAsyncProxy) {
    [super willChangeValueForKey:"async"];
    [crysonEntityAsyncProxy willChangeValueForKey:aKey];
  }
}

- (void)didChangeValueForKey:(CPString)aKey
{
  [super didChangeValueForKey:aKey];
  if (crysonEntityAsyncProxy) {
    [super didChangeValueForKey:"async"];
    [crysonEntityAsyncProxy didChangeValueForKey:aKey];
  }
}

- (CrysonEntityAsyncProxy)async
{
  if (!crysonEntityAsyncProxy) {
    crysonEntityAsyncProxy = [[CrysonEntityAsyncProxy alloc] initWithEntity:self];
  }
  return crysonEntityAsyncProxy;
}

@end
