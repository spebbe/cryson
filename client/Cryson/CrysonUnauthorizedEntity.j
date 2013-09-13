@import "CrysonEntity.j"

var UnauthorizedEntityDefinition = [CPDictionary dictionaryWithObject:@"Long" forKey:@"id"];

@implementation CrysonUnauthorizedEntity : CrysonEntity {

}

- (CPDictionary)definition
{
  return UnauthorizedEntityDefinition;
}

- (BOOL)isAuthorized
{
  return NO;
}

@end
