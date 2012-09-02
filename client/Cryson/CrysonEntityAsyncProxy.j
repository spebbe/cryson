@implementation CrysonEntityAsyncProxy : CPObject
{
  CrysonEntity        entity;
  BOOL                withinAsyncOperation @accessors;
  CPMutableDictionary loadingAssociations;
}

- (id)initWithEntity:(CrysonEntity)anEntity
{
  self = [super init];
  if (self) {
    entity = anEntity;
    loadingAssociations = [[CPMutableSet alloc] init];
  }
  return self;
}

- (BOOL)respondsToSelector:(SEL)aSelector
{
  return [entity respondsToSelector:aSelector];
}

- (CPMethodSignature)methodSignatureForSelector:(SEL)selector {
  return [entity methodSignatureForSelector:selector];
}

- (void)forwardInvocation:(CPInvocation)anInvocation {
  withinAsyncOperation = YES;
  [entity forwardInvocation:anInvocation];
  withinAsyncOperation = NO;
}

- (id)valueForKey:(CPString)aKey
{
  withinAsyncOperation = YES;
  var returnValue = [entity valueForKey:aKey];
  withinAsyncOperation = NO;
  return returnValue;
}

- (void)setValue:(id)aValue forKey:(CPString)aKey
{
  [entity setValue:aValue forKey:aKey];
}

- (id)loadAssociation:(CPString)associationName byClass:(CLASS)associationClass andIds:(CPArray)associationId
{
  if (![loadingAssociations containsObject:associationName]) {
    [loadingAssociations addObject:associationName];
    [[entity session] findByClass:associationClass andIds:associationId fetch:nil delegate:[self delegateWithAssociationName:associationName]];
  }
  return [];
}

- (id)loadAssociation:(CPString)associationName byClass:(CLASS)associationClass andId:(CPNumber)associationId
{
  if (![loadingAssociations containsObject:associationName]) {
    [loadingAssociations addObject:associationName];
    [[entity session] findByClass:associationClass andId:associationId fetch:nil delegate:[self delegateWithAssociationName:associationName]];  
  }
  return nil;
}

- (CrysonEntityAsyncProxyDelegate)delegateWithAssociationName:(CPString)anAssociationName
{
  return [[CrysonEntityAsyncProxyDelegate alloc] initWithEntity:entity associationName:anAssociationName];
}

@end

@implementation CrysonEntityAsyncProxyDelegate : CPObject
{
  CrysonEntity entity;
  CPString     associationName;
}

- (id)initWithEntity:(CrysonEntity)anEntity associationName:(CPString)anAssociationName
{
  self = [super init];
  if (self) {
    entity = anEntity;
    associationName = anAssociationName;
  }
  return self;
}

- (void)crysonSession:(CrysonSession)aSession found:(CrysonEntity)anEntity byClass:(CLASS)klazz
{
  [entity setValue:anEntity forKey:associationName];
}

- (void)crysonSession:(CrysonSession)aSession found:(CPArray)someEntities byClass:(CLASS)klazz andIds:(CPArray)someIds
{
  [entity setValue:someEntities forKey:associationName];
}

@end
