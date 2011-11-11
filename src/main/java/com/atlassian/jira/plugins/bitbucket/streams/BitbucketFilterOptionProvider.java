package com.atlassian.jira.plugins.bitbucket.streams;

import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.streams.api.ActivityObjectType;
import com.atlassian.streams.api.ActivityObjectTypes;
import com.atlassian.streams.api.ActivityVerb;
import com.atlassian.streams.api.ActivityVerbs;
import com.atlassian.streams.spi.StreamsFilterOption;
import com.atlassian.streams.spi.StreamsFilterOptionProvider;
import com.google.common.collect.ImmutableList;

import static com.atlassian.streams.api.ActivityObjectTypes.newTypeFactory;
import static com.atlassian.streams.api.ActivityVerbs.ATLASSIAN_IRI_BASE;
import static com.atlassian.streams.api.ActivityVerbs.newVerbFactory;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.atlassian.streams.api.ActivityVerbs.update;

/**
 * Implementing StreamsFilterOptionProvider can provide filtering options that will be displayed to the user in the stream gadget configuration.
 * For example, one filter implemented by the streams-jira-plugin is filtering by issue type (bug, task, etc).
 * <p/>
 * In this case, we are allowing filtering on the types of activities only. We have included filtering for the various kinds of log entries.
 */
public class BitbucketFilterOptionProvider implements StreamsFilterOptionProvider {
    private final I18nResolver i18nResolver;

    public BitbucketFilterOptionProvider(I18nResolver i18nResolver) {
        this.i18nResolver = checkNotNull(i18nResolver, "i18nResolver");
    }

    public Iterable<StreamsFilterOption> getFilterOptions() {
        //No additional filter options are implemented for this example plugin.
        return ImmutableList.of();
    }

    public Iterable<ActivityOption> getActivities() {
        //Let's return all of the different kinds of UPM log activities. That way we can filter by individual log entry types.
        ImmutableList.Builder<ActivityOption> activityOptions = ImmutableList.builder();
//        for (EntryType entryType : EntryType.values()) {
//            activityOptions.add(new ActivityOption(i18nResolver.getText(entryType.getI18nName()), BitbucketUPMActivityObjectTypes.bitbucketEvent(), BitbucketUPMActivityVerbs.getVerbFromEntryType(entryType)));
//        }

        return activityOptions.build();
    }

    /**
     * A way to create {@link ActivityVerb}s for UPM activities.
     */
    public static class BitbucketUPMActivityVerbs {
        private static final String EXAMPLE_IRI_BASE = ATLASSIAN_IRI_BASE + "bitbucket/";
        private static final ActivityVerbs.VerbFactory upmVerbs = newVerbFactory(EXAMPLE_IRI_BASE);

//        public static ActivityVerb getVerbFromEntryType(EntryType entryType) {
//            return upmVerbs.newVerb(entryType.name().toLowerCase(), update());
//        }

        public static ActivityVerb getVerb(String name) {
            return upmVerbs.newVerb(name.toLowerCase(), update());
        }
    }

    /**
     * A single {@link ActivityObjectType} for UPM activities.
     */
    public static final class BitbucketUPMActivityObjectTypes {
        static final String BITBUCKET_ACTIVITY_OBJECT_TYPE = "bitbucketEvent";
        private static final ActivityObjectTypes.TypeFactory bitbucketTypes = newTypeFactory(ActivityObjectTypes.ATLASSIAN_IRI_BASE);

        public static ActivityObjectType bitbucketEvent() {
            return bitbucketTypes.newType(BITBUCKET_ACTIVITY_OBJECT_TYPE);
        }
    }
}
