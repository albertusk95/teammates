package teammates.test.cases.pagedata;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.FeedbackQuestionAttributes;
import teammates.test.cases.BaseTestCase;
import teammates.ui.controller.InstructorFeedbackQuestionCopyPageData;
import teammates.ui.template.FeedbackQuestionCopyTable;

public class InstructorFeedbackQuestionCopyPageDataTest extends BaseTestCase {

    private static DataBundle dataBundle = getTypicalDataBundle();
    
    @Test
    public void allTests() {
        ______TS("Typical case");
        
        List<FeedbackQuestionAttributes> copiableQuestions = new ArrayList<FeedbackQuestionAttributes>();
        copiableQuestions.addAll(dataBundle.feedbackQuestions.values());
        
        InstructorFeedbackQuestionCopyPageData data = new InstructorFeedbackQuestionCopyPageData(
                dataBundle.accounts.get("instructor1OfCourse1"), copiableQuestions);
        FeedbackQuestionCopyTable copyForm = data.getCopyQnForm();
        assertEquals(dataBundle.feedbackQuestions.size(), copyForm.getQuestionRows().size());
    }
    
}
