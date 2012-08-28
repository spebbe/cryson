@import <AppKit/CPArrayController.j>

@implementation CPArrayController (SingleSelectionBinding)

- (CPObject)selectedObject
{
  var selectedObjects = [self selectedObjects];
  if ([selectedObjects count] == 0) {
    return nil;
  } else {
    return [[self selectedObjects] objectAtIndex:0];
  }
}

+ (CPSet)keyPathsForValuesAffectingSelectedObject
{
  return [CPSet setWithObjects:"selectedObjects"];
}

@end
