package com.cards.security;

public class SecurityUtils {
    public boolean isOwnerOfCard(Long cardId) {
        // TODO: implement using logged-in user’s customerId
        return true;
    }

    public boolean isCustomerOwner(Long customerId) {
        // TODO: implement for customer context
        return true;
    }
}
