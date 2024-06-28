import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CrptApi {

    // Scheduler to reset the request limit periodically
    private final ScheduledExecutorService scheduler;
    // Queue to limit the number of requests
    private final BlockingQueue<Runnable> queue;
    // Executor service to manage request submission
    private final ExecutorService executor;
    // Request limit and time unit for rate limiting
    private final int requestLimit;
    private final TimeUnit timeUnit;
    // Lock to ensure thread safety
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Constructor to initialize the CrptApi with specified time unit and request limit.
     * 
     * @param timeUnit - The time unit for rate limiting.
     * @param requestLimit - The maximum number of requests allowed in the specified time unit.
     */
    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.queue = new ArrayBlockingQueue<>(requestLimit);
        this.executor = new ThreadPoolExecutor(requestLimit, requestLimit, 0L, TimeUnit.MILLISECONDS, queue);
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.scheduler.scheduleAtFixedRate(this::resetQueue, 0, 1, timeUnit);
    }

    /**
     * Method to clear the request queue, resetting the request count.
     */
    private void resetQueue() {
        queue.clear();
    }

    /**
     * Method to create a document, ensuring that the request limit is not exceeded.
     * 
     * @param document - The document object to be sent to the API.
     * @param signature - The signature for the document.
     * @throws InterruptedException - if the thread is interrupted while waiting.
     */
    public void createDocument(Document document, String signature) throws InterruptedException {
        lock.lock();
        try {
            executor.submit(() -> {
                try {
                    sendRequest(document, signature);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            });
        } finally {
            lock.unlock();
        }
    }

    /**
     * Method to send the request to the API.
     * 
     * @param document - The document object to be sent.
     * @param signature - The signature for the document.
     * @throws IOException - if an I/O error occurs.
     * @throws InterruptedException - if the operation is interrupted.
     */
    private void sendRequest(Document document, String signature) throws IOException, InterruptedException {
        // Convert the document object to JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(document);

        // Create the HTTP client and request
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        // Send the request and print the response
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Response: " + response.body());
    }

    public static void main(String[] args) throws InterruptedException {
        // Create an instance of CrptApi with a limit of 10 requests per minute
        CrptApi api = new CrptApi(TimeUnit.MINUTES, 10);

        // Create a sample document
        Document document = new Document("string", "string", "string", "LP_INTRODUCE_GOODS",
                true, "string", "string", "string", "2020-01-23", "string", 
                new Product[] { new Product("string", "2020-01-23", "string", "string", "string", "2020-01-23", "string", "string", "string") },
                "2020-01-23", "string");

        // Call the createDocument method
        api.createDocument(document, "signature");
    }

    // Inner class representing the document to be sent to the API
    static class Document {
        public Description description;
        public String doc_id;
        public String doc_status;
        public String doc_type;
        public boolean importRequest;
        public String owner_inn;
        public String participant_inn;
        public String producer_inn;
        public String production_date;
        public String production_type;
        public Product[] products;
        public String reg_date;
        public String reg_number;

        public Document(String participantInn, String doc_id, String doc_status, String doc_type,
                        boolean importRequest, String owner_inn, String participant_inn, String producer_inn,
                        String production_date, String production_type, Product[] products, String reg_date, String reg_number) {
            this.description = new Description(participantInn);
            this.doc_id = doc_id;
            this.doc_status = doc_status;
            this.doc_type = doc_type;
            this.importRequest = importRequest;
            this.owner_inn = owner_inn;
            this.participant_inn = participant_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.production_type = production_type;
            this.products = products;
            this.reg_date = reg_date;
            this.reg_number = reg_number;
        }
    }

    // Inner class representing the description part of the document
    static class Description {
        public String participantInn;

        public Description(String participantInn) {
            this.participantInn = participantInn;
        }
    }

    // Inner class representing a product in the document
    static class Product {
        public String certificate_document;
        public String certificate_document_date;
        public String certificate_document_number;
        public String owner_inn;
        public String producer_inn;
        public String production_date;
        public String tnved_code;
        public String uit_code;
        public String uitu_code;

        public Product(String certificate_document, String certificate_document_date, String certificate_document_number,
                       String owner_inn, String producer_inn, String production_date, String tnved_code,
                       String uit_code, String uitu_code) {
            this.certificate_document = certificate_document;
            this.certificate_document_date = certificate_document_date;
            this.certificate_document_number = certificate_document_number;
            this.owner_inn = owner_inn;
            this.producer_inn = producer_inn;
            this.production_date = production_date;
            this.tnved_code = tnved_code;
            this.uit_code = uit_code;
            this.uitu_code = uitu_code;
        }
    }
}
