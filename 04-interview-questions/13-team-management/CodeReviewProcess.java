import java.util.ArrayList;
import java.util.List;

/**
 * This class models a Code Review process, a common area for team friction.
 * A lead engineer establishes and enforces principles to ensure reviews are
 * constructive, objective, and conflict-free, fostering effective team management.
 */
public class CodeReviewProcess {

    private final List<String> comments = new ArrayList<>();
    private final ReviewPrinciples principles;

    public CodeReviewProcess(ReviewPrinciples principles) {
        this.principles = principles;
    }

    public void addComment(String author, String comment) {
        if (principles.isConstructive(comment)) {
            comments.add(author + ": " + comment);
        } else {
            System.err.println("Rejected comment from " + author + ": Not constructive. " + principles.getGuidance());
        }
    }

    public void printReview() {
        System.out.println("--- Code Review Comments ---");
        comments.forEach(System.out::println);
        System.out.println("--------------------------");
    }

    /**
     * A set of principles a lead would establish for code reviews.
     */
    static class ReviewPrinciples {
        public boolean isConstructive(String comment) {
            // Principle 1: Comment on the code, not the author.
            if (comment.toLowerCase().contains("your code") || comment.toLowerCase().contains("you did")) {
                return false;
            }
            // Principle 2: Be specific and provide actionable suggestions or ask clarifying questions.
            if (!comment.contains("?") && !comment.toLowerCase().contains("consider")) {
                return false;
            }
            // Principle 3: Avoid demanding language.
            if (comment.toLowerCase().contains("you must") || comment.toLowerCase().contains("change this now")) {
                return false;
            }
            return true;
        }

        public String getGuidance() {
            return "Guidance: Frame comments as questions or suggestions about the code itself (e.g., 'Consider using a different pattern here because...').";
        }
    }

    public static void main(String[] args) {
        // A lead engineer socializes these principles with the team.
        ReviewPrinciples teamPrinciples = new ReviewPrinciples();
        CodeReviewProcess review = new CodeReviewProcess(teamPrinciples);

        System.out.println("Simulating a code review...");

        // Good, constructive comment
        review.addComment("Alice", "Consider extracting this logic into a separate utility method to improve reusability. What do you think?");
        
        // Bad, confrontational comment (will be rejected)
        review.addComment("Bob", "Why did you write this code? It's terrible.");

        // Bad, demanding comment (will be rejected)
        review.addComment("Charlie", "You must change this to use the Strategy pattern now.");

        // Good, questioning comment
        review.addComment("Alice", "I'm not sure I understand the goal of this section. Could we clarify the requirements?");

        review.printReview();
    }
}
