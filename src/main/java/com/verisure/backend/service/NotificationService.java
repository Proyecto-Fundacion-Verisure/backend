package com.verisure.backend.service;

public interface NotificationService {

    void notifyRegistrationConfirmed(Long registrationId);
    

    void notifyRegistrationWaitlisted(Long registrationId);


    void notifyRegistrationRejected(Long registrationId);


    void notifySpotReleased(Long registrationId);

    
    void notifyActivityCancelled(Long activityId);


    void notifyActivityFinished(Long activityId);

    
    void notifyActivityClosed(Long activityId);

    
    void notifyActivitySubmittedForReview(Long activityId);

    
    void notifyActivityApproved(Long activityId);

    
    void notifyActivityReturned(Long activityId);

    
    void notifyOrgAccountApproved(Long userId);

   
    void notifyOrgAccountRejected(Long userId);

 



}
