package org.sgnn7.ourobo.test;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sgnn7.ourobo.MainActivity;
import org.sgnn7.ourobo.R;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

	@Rule
	public ActivityScenarioRule<MainActivity> activityRule =
			new ActivityScenarioRule<>(MainActivity.class);

	@Test
	public void mainActivityLaunches() {
		onView(withId(R.id.posts_list)).check(matches(isDisplayed()));
	}

	@Test
	public void subredditSpinnerIsDisplayed() {
		onView(withId(R.id.subreddit_spinner)).check(matches(isDisplayed()));
	}

	@Test
	public void fabMenuIsDisplayed() {
		onView(withId(R.id.fab_menu)).check(matches(isDisplayed()));
	}
}
