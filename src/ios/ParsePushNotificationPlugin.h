//
//  ParsePushNotificationPlugin.h
//  HelloWorld
//
//  Created by yoyo on 2/12/14.
//
//	Updated by avivais on 9/12/14.

#import <Cordova/CDV.h>

@interface ParsePushNotificationPlugin : CDVPlugin {
        NSMutableArray* pendingNotifications;
}
    
@property (nonatomic, copy) NSString *callbackId;
@property (nonatomic, retain) NSMutableArray* pendingNotifications;
    
- (void)register:(CDVInvokedUrlCommand*)command;
- (void)unregister:(CDVInvokedUrlCommand*)command;
- (void)getInstallationId:(CDVInvokedUrlCommand*)command;
- (void)getEndUserId:(CDVInvokedUrlCommand*)command;
- (void)getNotifications:(CDVInvokedUrlCommand*)command;
- (void)getSubscriptions:(CDVInvokedUrlCommand*)command;
- (void)subscribeToChannel:(CDVInvokedUrlCommand*)command;
- (void)unsubscribeFromChannel:(CDVInvokedUrlCommand*)command;
- (void)isPushAllowed:(CDVInvokedUrlCommand*)command;
- (void)didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken;
- (void)didFailToRegisterForRemoteNotificationsWithError:(NSError *)error;
- (void)didReceiveRemoteNotificationWithPayload:(NSDictionary *)payload;
    
@end
