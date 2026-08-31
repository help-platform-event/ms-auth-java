package com.maxime.help.msauth.domain.model;

/**
 * Profile data for a {@link User}. Part of the User aggregate — never persisted on its own.
 * Mutated only through the intention-revealing methods below; blank input is normalised to
 * {@code null}.
 */
public class Profile {

    private String firstName;
    private String lastName;
    private String avatarUrl;
    private String phone;
    private String bio;
    private Address address;

    private Profile() {
    }

    /** A blank profile, as created alongside a brand-new user. */
    public static Profile empty() {
        return new Profile();
    }

    /** Rebuilds a profile from persisted state. Infrastructure use only. */
    public static Profile reconstitute(
            String firstName,
            String lastName,
            String avatarUrl,
            String phone,
            String bio,
            Address address) {
        Profile profile = new Profile();
        profile.firstName = firstName;
        profile.lastName = lastName;
        profile.avatarUrl = avatarUrl;
        profile.phone = phone;
        profile.bio = bio;
        profile.address = address;
        return profile;
    }

    public void changeName(String firstName, String lastName) {
        this.firstName = blankToNull(firstName);
        this.lastName = blankToNull(lastName);
    }

    public void changePhone(String phone) {
        this.phone = blankToNull(phone);
    }

    public void changeAvatarUrl(String avatarUrl) {
        this.avatarUrl = blankToNull(avatarUrl);
    }

    public void changeBio(String bio) {
        this.bio = blankToNull(bio);
    }

    public void changeAddress(Address address) {
        this.address = (address == null || address.isEmpty()) ? null : address;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getPhone() {
        return phone;
    }

    public String getBio() {
        return bio;
    }

    public Address getAddress() {
        return address;
    }
}
