package com.finsight.news.application;

import com.finsight.news.persistence.FinancialNewsEntity;
import com.finsight.news.persistence.FinancialNewsRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Seeds clearly labeled practice scenarios when local mock market data is enabled. */
@Component
@ConditionalOnProperty(name = "finsight.news.provider", havingValue = "mock", matchIfMissing = true)
public class DemoFinancialNewsSeeder implements ApplicationRunner {
    private static final String SOURCE = "DEMO PRACTICE";
    private static final String DEMO_URL = "https://example.com/";

    private final FinancialNewsRepository news;
    private final Clock clock;

    public DemoFinancialNewsSeeder(FinancialNewsRepository news, Clock clock) {
        this.news = news;
        this.clock = clock;
    }

    @Override
    public void run(ApplicationArguments args) {
        Instant now = clock.instant();
        List<PracticeStory> stories = List.of(
                new PracticeStory("rates-and-bonds",
                        "Practice scenario: How a rate cut can affect bond prices",
                        "This simulated example explores how existing fixed-rate bond prices may respond when market yields move. Actual outcomes depend on duration, credit quality, inflation expectations and market conditions. No real policy announcement is being reported.",
                        now.minus(Duration.ofHours(2))),
                new PracticeStory("sales-and-costs",
                        "Practice company update: Sales grow while costs rise faster",
                        "In this fictional company example, sales increase but operating costs grow faster. It is a reminder to compare revenue with margins, cash flow and debt instead of judging a business from sales growth alone. These figures and events are not real.",
                        now.minus(Duration.ofHours(7))),
                new PracticeStory("market-diversification",
                        "Practice market note: Diversification during a sell-off",
                        "This mock market scenario describes a broad decline across several investments. Diversification can spread exposure, but it cannot prevent losses or guarantee a profit. This is an educational scenario, not a report of current market activity.",
                        now.minus(Duration.ofHours(14))),
                new PracticeStory("dividend-yield",
                        "Practice investing brief: Read a dividend yield carefully",
                        "A high dividend yield can result from a falling share price as well as a larger payout. Investors often review the company’s cash flow, payout ratio and dividend history before drawing conclusions. This is a learning example, not company-specific news.",
                        now.minus(Duration.ofHours(22)))
        );

        for (PracticeStory story : stories) {
            String externalId = "finsight-demo-" + story.slug();
            if (news.existsBySourceAndExternalId(SOURCE, externalId)) continue;

            FinancialNewsEntity item = new FinancialNewsEntity(UUID.randomUUID().toString(), null, SOURCE,
                    externalId, story.title(), DEMO_URL, story.publishedAt(), story.summary(), now);
            item.setSummary(story.summary(), "DEMO", now);
            news.save(item);
        }
    }

    private record PracticeStory(String slug, String title, String summary, Instant publishedAt) { }
}
