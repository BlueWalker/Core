package walker.blue.core.lib.main;

import walker.blue.core.lib.user.UserState;

/**
 * Handler which defines behaior when the state of the user is found
 */
public interface UserStateHandler {
    public void newStateFound(final UserState userState);
}