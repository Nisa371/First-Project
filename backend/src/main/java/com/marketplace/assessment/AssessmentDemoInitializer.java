package com.marketplace.assessment;
import com.marketplace.candidate.CandidateType;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name="app.assessments.seed-demo",havingValue="true",matchIfMissing=true)
public class AssessmentDemoInitializer implements ApplicationRunner {
    private final AssessmentRepository assessments;
    private final AssessmentQuestionRepository questions;
    @Override @Transactional public void run(ApplicationArguments args) {
        seed(CandidateType.TECH,"TECH foundations · Demo",new String[][] {
            {"Which structure processes items in first-in, first-out order?","Stack","Queue","Tree","Set","B"},
            {"Which HTTP status indicates an unauthenticated request?","200","404","401","500","C"},
            {"What protects a multi-record database operation from partial completion?","A transaction","A CSS rule","A cache key","A redirect","A"},
            {"Where must authorization be enforced?","Only in the browser","Only in navigation","Only in CSS","On the backend","D"}
        });
        seed(CandidateType.TRADE,"কাজের নিরাপত্তা · Safety conversation",new String[][] {
            {"বৈদ্যুতিক কাজ শুরুর আগে কী করবেন? / Before electrical work?","বিদ্যুৎ বন্ধ ও পরীক্ষা / Isolate and test","সরাসরি শুরু / Start immediately","পানি দিন / Add water","গ্লাভস খুলুন / Remove gloves","A"},
            {"কাজে বিপদ দেখলে কী করবেন? / If you notice a hazard?","উপেক্ষা / Ignore","দ্রুত কাজ / Rush","থামুন ও জানান / Stop and report","লুকান / Hide it","C"}
        });
    }
    private void seed(CandidateType type,String title,String[][] data) {
        if(assessments.findAll().stream().anyMatch(a -> a.getTitle().equals(title))) return;
        var a=new Assessment(); a.setTitle(title); a.setCandidateType(type); a.setPassingScore(BigDecimal.valueOf(70)); assessments.save(a);
        for(var row:data) { var q=new AssessmentQuestion(); q.setAssessment(a); q.setPrompt(row[0]); q.setOptionA(row[1]);
            q.setOptionB(row[2]); q.setOptionC(row[3]); q.setOptionD(row[4]); q.setCorrectOption(AnswerOption.valueOf(row[5])); questions.save(q); }
    }
}
