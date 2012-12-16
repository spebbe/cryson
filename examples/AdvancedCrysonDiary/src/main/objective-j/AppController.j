@import <Foundation/CPObject.j>
@import <Cryson/Cryson.j>
@import "Models/Entry.j"
@import "Models/EntryContent.j"
@import "Models/EntryComment.j"

@import "CPArrayController_SingleSelectionBinding.j"

@implementation AppController : CPObject
{
  @outlet CPWindow            window;
  @outlet CPArrayController   entriesArrayController;
  @outlet CPObjectController  entryObjectController;
  @outlet CPArrayController   entryCommentsArrayController;

  @outlet CPTextField         diaryEntriesLabel;

  @outlet CPTextField         loadingLabel;

  CrysonSessionFactory        crysonSessionFactory;
  CrysonSession               crysonSession;

  CPArray                     originalEntries;
  CPMutableArray              entries;

  CPString                    userName;
}

- (void)applicationDidFinishLaunching:(CPNotification)aNotification
{
  [RemoteService get:@"/services/diary/login" delegate:self onSuccess:@selector(userDidLogin:)];
}

- (void)userDidLogin:(CPString)aUserName
{
  userName = aUserName;
  [diaryEntriesLabel setObjectValue:userName + @"'s diary entries"];
  crysonSessionFactory = [[CrysonSessionFactory alloc] initWithBaseUrl:@"/cryson"];
  crysonSession = [crysonSessionFactory createSessionWithDelegate:self];
  [crysonSession findAllByClass:Entry fetch:nil];
}

- (void)awakeFromCib
{
  [window setFullPlatformWindow:YES];
  [window setBackgroundColor:[CPColor colorWithCalibratedRed:0.93 green:0.93 blue:0.93 alpha:1.0]];

  [entriesArrayController setSortDescriptors:[[CPSortDescriptor sortDescriptorWithKey:@"date" ascending:NO]]];
}

- (void)crysonSession:(CrysonSession)aSession foundAll:(CPArray)entities byClass:(CLASS)aClass
{
  originalEntries = [entities copy];
  entries = [originalEntries mutableCopy];
  [entriesArrayController setContent:entries];
}

- (void)crysonSessionCommitted:(CrysonSession)aSession
{
  originalEntries = [entries copy];
}

- (void)crysonSession:(CrysonSession)aCrysonSession commitFailedWithError:(CrysonError)error
{
  [[CPAlert alertWithError:@"Could not save: " + [error message]] runModal];
}

- (void)crysonSessionDidStartLoadOperation:(CrysonSession)aSession
{
  [loadingLabel setHidden:NO];
}

- (void)crysonSessionDidFinishLoadOperation:(CrysonSession)aSession
{
  [loadingLabel setHidden:YES];
}

- (@action)save:(id)sender
{
  [crysonSession commit];
}

- (@action)revert:(id)sender
{
  [crysonSession rollback];
  entries = [originalEntries mutableCopy];
  [entriesArrayController setContent:entries];
}

- (@action)newEntry:(id)sender
{
  var entry = [[Entry alloc] initWithSession:crysonSession];
  var entryContent = [[EntryContent alloc] initWithSession:crysonSession];

  [entry setTitle:@"New Entry"];
  [entry setDate:[[[CPDate alloc] init] description]];
  [entry setContent:entryContent];
  [entry setComments:[CPMutableArray array]];
  [entry setUserName:userName];

  [entryContent setText:@""];
  [entryContent setEntries:[entry]];

  [entriesArrayController addObject:entry];
}

- (@action)newComment:(id)sender
{
  var comment = [[EntryComment alloc] initWithSession:crysonSession];
  [comment setCreated:[[[CPDate alloc] init] description]];
  [comment setText:@""];
  [comment setEntry:[entryObjectController content]];
  
  [entryCommentsArrayController addObject:comment];
}

@end
