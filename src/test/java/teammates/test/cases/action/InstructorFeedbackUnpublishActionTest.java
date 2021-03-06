package teammates.test.cases.action;

import java.util.Date;
import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.FeedbackSessionAttributes;
import teammates.common.util.Const;
import teammates.common.util.Const.ParamsNames;
import teammates.common.util.TaskWrapper;
import teammates.common.util.TimeHelper;
import teammates.storage.api.FeedbackSessionsDb;
import teammates.ui.controller.InstructorFeedbackUnpublishAction;
import teammates.ui.controller.RedirectResult;

public class InstructorFeedbackUnpublishActionTest extends BaseActionTest {
    private static final boolean PUBLISHED = true;
    private static final boolean UNPUBLISHED = false;
    private final DataBundle dataBundle = getTypicalDataBundle();

    @BeforeClass
    public void classSetup() {
        printTestClassHeader();
        removeAndRestoreTypicalDataBundle();
        uri = Const.ActionURIs.INSTRUCTOR_FEEDBACK_UNPUBLISH;
    }

    @Test
    public void testExecuteAndPostProcess() throws Exception {
        gaeSimulation.loginAsInstructor(dataBundle.instructors.get("instructor1OfCourse1").googleId);
        FeedbackSessionAttributes session = dataBundle.feedbackSessions.get("session2InCourse1");

        String[] paramsNormal = {
                Const.ParamsNames.COURSE_ID, session.getCourseId(),
                Const.ParamsNames.FEEDBACK_SESSION_NAME, session.getFeedbackSessionName()
        };
        String[] paramsWithNullCourseId = {
                Const.ParamsNames.FEEDBACK_SESSION_NAME, session.getFeedbackSessionName()
        };
        String[] paramsWithNullFeedbackSessionName = {
                Const.ParamsNames.COURSE_ID, session.getCourseId()
        };

        ______TS("Typical successful case: session unpublishable");

        makeFeedbackSessionPublished(session);

        InstructorFeedbackUnpublishAction unpublishAction = getAction(paramsNormal);
        RedirectResult result = (RedirectResult) unpublishAction.executeAndPostProcess();

        String expectedDestination = Const.ActionURIs.INSTRUCTOR_FEEDBACKS_PAGE + "?error=false"
                                     + "&user=idOfInstructor1OfCourse1";
        assertEquals(expectedDestination, result.getDestinationWithParams());
        assertEquals(Const.StatusMessages.FEEDBACK_SESSION_UNPUBLISHED, result.getStatusMessage());
        assertFalse(result.isError);
        
        verifySpecifiedTasksAdded(unpublishAction, Const.TaskQueue.FEEDBACK_SESSION_UNPUBLISHED_EMAIL_QUEUE_NAME, 1);
        
        TaskWrapper taskAdded = unpublishAction.getTaskQueuer().getTasksAdded().get(0);
        Map<String, String[]> paramMap = taskAdded.getParamMap();
        assertEquals(session.getCourseId(), paramMap.get(ParamsNames.EMAIL_COURSE)[0]);
        assertEquals(session.getSessionName(), paramMap.get(ParamsNames.EMAIL_FEEDBACK)[0]);

        ______TS("Unsuccessful case 1: params with null course id");

        String errorMessage = "";
        unpublishAction = getAction(paramsWithNullCourseId);

        try {
            unpublishAction.executeAndPostProcess();
            signalFailureToDetectException("AssertionError expected");
        } catch (AssertionError e) {
            errorMessage = e.getMessage();
        }

        assertEquals(Const.StatusCodes.NULL_PARAMETER, errorMessage);

        ______TS("Unsuccessful case 2: params with null feedback session name");

        errorMessage = "";
        unpublishAction = getAction(paramsWithNullFeedbackSessionName);

        try {
            unpublishAction.executeAndPostProcess();
            signalFailureToDetectException("AssertionError expected");
        } catch (AssertionError e) {
            errorMessage = e.getMessage();
        }

        assertEquals(Const.StatusCodes.NULL_PARAMETER, errorMessage);

        ______TS("Unsuccessful case 3: trying to unpublish a session not currently published");

        makeFeedbackSessionUnpublished(session);

        unpublishAction = getAction(paramsNormal);
        result = (RedirectResult) unpublishAction.executeAndPostProcess();

        expectedDestination = Const.ActionURIs.INSTRUCTOR_FEEDBACKS_PAGE + "?error=true"
                              + "&user=idOfInstructor1OfCourse1";
        assertEquals(expectedDestination, result.getDestinationWithParams());
        assertEquals("Error unpublishing feedback session: Session has already been unpublished.",
                     result.getStatusMessage());
        assertTrue(result.isError);
        
        verifyNoTasksAdded(unpublishAction);

        makeFeedbackSessionPublished(session);
    }

    private void modifyFeedbackSessionPublishState(FeedbackSessionAttributes session, boolean isPublished) throws Exception {
        // startTime < endTime <= resultsVisibleFromTime
        Date startTime = TimeHelper.getDateOffsetToCurrentTime(-2);
        Date endTime = TimeHelper.getDateOffsetToCurrentTime(-1);
        Date resultsVisibleFromTimeForPublishedSession = TimeHelper.getDateOffsetToCurrentTime(-1);

        session.setStartTime(startTime);
        session.setEndTime(endTime);

        if (isPublished) {
            session.setResultsVisibleFromTime(resultsVisibleFromTimeForPublishedSession);
            assertTrue(session.isPublished());
        } else {
            session.setResultsVisibleFromTime(Const.TIME_REPRESENTS_LATER);
            assertFalse(session.isPublished());
        }

        session.setSentPublishedEmail(true);

        new FeedbackSessionsDb().updateFeedbackSession(session);
    }

    private void makeFeedbackSessionPublished(FeedbackSessionAttributes session) throws Exception {
        modifyFeedbackSessionPublishState(session, PUBLISHED);
    }

    private void makeFeedbackSessionUnpublished(FeedbackSessionAttributes session) throws Exception {
        modifyFeedbackSessionPublishState(session, UNPUBLISHED);
    }

    private InstructorFeedbackUnpublishAction getAction(String[] params) {
        return (InstructorFeedbackUnpublishAction) gaeSimulation.getActionObject(uri, params);
    }
}
