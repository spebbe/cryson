@import <Cryson/Cryson.j>

@implementation Entry : CrysonEntity
{
}

+ (CPSet)keyPathsForValuesAffectingContent
{
  return [CPSet setWithObjects:@"contents"];
}

- (EntryContent)content
{
  return [[self contents] objectAtIndex:0];
}

- (void)setContent:(EntryContent)entryContent
{
  [self setContents:[entryContent]];
}

@end
