package com.louise.androcamx;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class Contact extends Fragment {
    private ImageView person1LinkedinIcon;
    private ImageView person1GmailIcon;
    private ImageView person1GithubIcon;
    private ImageView person2LinkedinIcon;
    private ImageView person2GmailIcon;
    private ImageView person2GithubIcon;

    private static final String PERSON_1_LINKEDIN_URL = "https://www.linkedin.com/in/louise-patra-107148229/";
    private static final String PERSON_1_GMAIL_EMAIL = "rocklouis055@gmail.com";
    private static final String PERSON_1_GITHUB_URL = "https://github.com/rocklouis055";
    private static final String PERSON_2_LINKEDIN_URL = "https://www.linkedin.com/in/manisha-kumari-650b4b197/";
    private static final String PERSON_2_GMAIL_EMAIL = "manisha240303@gmail.com";
    private static final String PERSON_2_GITHUB_URL = "https://github.com/Technocharm";
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_contact, container, false);

        person1LinkedinIcon = view.findViewById(R.id.llinked);
        person1GmailIcon = view.findViewById(R.id.lmail);
        person1GithubIcon = view.findViewById(R.id.lgit);
        person2LinkedinIcon = view.findViewById(R.id.mlinked);
        person2GmailIcon = view.findViewById(R.id.mmail);
        person2GithubIcon = view.findViewById(R.id.mgit);
        person1LinkedinIcon.setOnClickListener(v -> openWebPage(PERSON_1_LINKEDIN_URL));
        person1GmailIcon.setOnClickListener(v -> openEmail(PERSON_1_GMAIL_EMAIL));
        person1GithubIcon.setOnClickListener(v -> openWebPage(PERSON_1_GITHUB_URL));
        person2LinkedinIcon.setOnClickListener(v -> openWebPage(PERSON_2_LINKEDIN_URL));
        person2GmailIcon.setOnClickListener(v -> openEmail(PERSON_2_GMAIL_EMAIL));
        person2GithubIcon.setOnClickListener(v -> openWebPage(PERSON_2_GITHUB_URL));
        return view;
    }

    private void openWebPage(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    private void openEmail(String email) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + email));
        startActivity(intent);
    }
    public String getTitle() {
        return "Contact Us";
    }
}