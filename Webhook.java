import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Webhook {
    private static final String SLACK_BASE_URL = "https://hooks.slack.com/services/";
    private static final String GROQ_BASE_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String TOGETHER_BASE_URL = "https://api.together.xyz/v1/images/generations";

    public static void main(String[] args) {
        String prompt = "너는 이제 KPOP 가수 카리나야. 점심에 배달시켜 먹기 좋은 메뉴를 가수 카리나의 말투를 사용해서 추천하고 마크다운 문법으로 작성하지 않고 평문으로 출력해줘.";
        String chain1 = requestTextWithLLM("llama-3.3-70b-versatile", prompt);
        String chain2 = requestTextWithLLM("llama-3.3-70b-versatile", chain1 + " 의 내용에 적힌 메뉴 중 하나를 랜덤으로 골라서 그려줘.");

        String imagePrompt = "가수 카리나를 그려줘.";
        String image = requestImageWithLLM("black-forest-labs/FLUX.1-schnell-Free", imagePrompt);

        sendSlackMsg("😋카리나의 점메추", chain1, image);
    }

    public static String requestImageWithLLM(String model, String prompt) {
        String apiUrl = TOGETHER_BASE_URL;
        String apiKey = System.getenv("TOGETHER_API_KEY");
        String payload = """
                {
                    "prompt": "%s",
                    "model": "%s",
                    "width": 1440,
                    "height": 1440,
                    "steps": 4,
                    "n": 1
                }
                """.formatted(prompt, model);

        HttpClient client = HttpClient.newHttpClient(); // 요청할 클라이언트 생성
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        String result = "";

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            result = response.body()
                    .split("\"url\": \"")[1]
                    .split("\",")[0];

            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());
            System.out.println("result = " + result);

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String requestTextWithLLM(String model, String prompt) {
        String apiUrl = GROQ_BASE_URL;
        String apiKey = System.getenv("GROQ_API_KEY");
        String payload = """
                {
                    "messages": [
                      {
                        "role": "user",
                        "content": "%s"
                      }
                    ],
                    "model": "%s",
                    "temperature": 1,
                    "max_completion_tokens": 1024,
                    "top_p": 1,
                    "stream": false,
                    "stop": null
                }
                """.formatted(prompt, model);

        HttpClient client = HttpClient.newHttpClient(); // 요청할 클라이언트 생성
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        String result = "";

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            result = response.body()
                    .split("\"content\":\"")[1]
                    .split("\"},\"logprobs\"")[0];

            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());
            System.out.println("result = " + result);

            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendSlackMsg(String text) {
        String slackUrl = SLACK_BASE_URL + System.getenv("SLACK_WEBHOOK_URL");
        String payload = "{\"text\": \"" + text + "\"}";

        HttpClient client = HttpClient.newHttpClient(); // 요청할 클라이언트 생성
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(slackUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendSlackMsg(String title, String text, String image) {
        String slackUrl = SLACK_BASE_URL + System.getenv("SLACK_WEBHOOK_URL");
        String payload = """
                    {"attachments": [{
                        "title": "%s",
                        "text": "%s",
                        "image_url": "%s"
                    }]}
                """.formatted(title, text, image);
        HttpClient client = HttpClient.newHttpClient(); // 요청할 클라이언트 생성
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(slackUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("response.statusCode() = " + response.statusCode());
            System.out.println("response.body() = " + response.body());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
