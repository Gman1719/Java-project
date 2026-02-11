package utils; 

/**
 * Interface for controllers that need to be notified when the user profile 
 * has been successfully updated in the EditProfileController.
 */
public interface ProfileUpdateListener {
    void onProfileUpdated();
}