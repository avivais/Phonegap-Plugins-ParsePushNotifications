//
//  AppDelegate+parsePushNotification.m
//  HelloWorld
//
//  Created by yoyo on 2/12/14.
//  Updated by avivais on 1/1/15.
//
//

#import "AppDelegate+parsePushNotification.h"

#import "ParsePushNotificationPlugin.h"
#import <objc/runtime.h>
#import <Parse/Parse.h>


@implementation AppDelegate (parsePushNotification)

+ (void) load
{
    Method original, swizzled;

    original = class_getInstanceMethod(self, @selector(init));
    swizzled = class_getInstanceMethod(self, @selector(swizzled_init));
    method_exchangeImplementations(original, swizzled);
}

- (AppDelegate *) swizzled_init
{
    [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(parseTrackAppOpen:)
                                                 name:@"UIApplicationDidFinishLaunchingNotification" object:nil];

    // This actually calls the original init method over in AppDelegate. Equivilent to calling super
    // on an overrided method, this is not recursive, although it appears that way. neat huh?
    return [self swizzled_init];
}

- (void) parseTrackAppOpen:(NSNotification *)notification
{
    if (notification)
    {
        NSDictionary *launchOptions = [notification userInfo];
        [Parse setApplicationId:@"r7aHNhWLVVZeLMj36TngO5j8pUrgAtx3CkwhGuW6" clientKey:@"WXBNgQEIplGqUfDGQliScPXvPo21mzWPi1Z2wttb"];
        [PFAnalytics trackAppOpenedWithLaunchOptions:launchOptions];
    }
}

- (void) application:(UIApplication *)application didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken
{
    ParsePushNotificationPlugin *pushHandler = [self getCommandInstance:@"ParsePushNotificationPlugin"];
    [pushHandler didRegisterForRemoteNotificationsWithDeviceToken:deviceToken];
}

- (void) application:(UIApplication *)application didFailToRegisterForRemoteNotificationsWithError:(NSError *)error
{
    ParsePushNotificationPlugin *pushHandler = [self getCommandInstance:@"ParsePushNotificationPlugin"];
    [pushHandler didFailToRegisterForRemoteNotificationsWithError:error];
}

- (void) application:(UIApplication *)application didReceiveRemoteNotification:(NSDictionary *)payload
{
    
    NSLog(@"didReceiveRemoteNotification");
    UIApplicationState appstate = [[UIApplication sharedApplication] applicationState];
    
    NSMutableDictionary *extendedPayload = [payload mutableCopy];
    [extendedPayload setObject:[NSNumber numberWithBool:(appstate == UIApplicationStateActive)] forKey:@"receivedInForeground"];
    
    ParsePushNotificationPlugin *pushHandler = [self getCommandInstance:@"ParsePushNotificationPlugin"];
    [pushHandler didReceiveRemoteNotificationWithPayload:extendedPayload];
}

- (id) getCommandInstance:(NSString*)className
{
    return [self.viewController getCommandInstance:className];
}

@end
