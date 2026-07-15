void main() {
    List<Book> books = List.of(new Book(1, "Java"), new Book(2, "Obitaemii"), new Book(3, "Grad"));

    SubmissionPublisher<Book> bookPublisher = new SubmissionPublisher<>();

    BookSubscriber sub1 = new BookSubscriber("SUB1");
    BookSubscriber sub2 = new BookSubscriber("SUB2");
    bookPublisher.subscribe(sub1);
    bookPublisher.subscribe(sub2);

    System.out.println("Submit method");
    books.forEach(book -> {
        bookPublisher.submit(book);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    });

    System.out.println("Offer method");

    books.forEach(book ->
    {
        bookPublisher.offer(book, null);

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    });


    System.out.println("Offer method with timeout");

    books.forEach(book ->
    {
        bookPublisher.offer(book, 2, TimeUnit.SECONDS, ((subscriber, book1) -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return true;
        }));
    });

    bookPublisher.close();

    try {
        Thread.sleep(1000);
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
}


record Book(int bookId, String bookName)
{
    @Override
    public String toString()
    {
        return "Book [bookID=" + bookId + ", bookName=" + bookName + "]";
    }
}

class BookSubscriber implements Flow.Subscriber<Book>
{
    public BookSubscriber(String subscriberName) {
        this.subscriberName = subscriberName;
    }

    private String subscriberName;
    private Flow.Subscription subscription;

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.subscription = subscription;
        subscription.request(2);
    }

    @Override
    public void onNext(Book item) {
        subscription.request(2);
        System.out.println(item + ", Received by - " + subscriberName);
    }

    @Override
    public void onError(Throwable throwable) {
        System.out.println(subscriberName + " ERROR: " + throwable.getMessage());
    }

    @Override
    public void onComplete() {
        System.out.println(subscriberName + "got all the books");
    }
}