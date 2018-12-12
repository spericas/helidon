package io.helidon.webserver.multipart;

import io.helidon.common.Builder;
import io.helidon.common.http.BodyPartHeaders;
import io.helidon.common.http.Content;
import io.helidon.common.http.DataChunk;
import io.helidon.common.http.MultiPartDataChunk;
import io.helidon.common.reactive.Flow;
import io.helidon.webserver.Request;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;

/**
 * Class BodyPart.
 *
 * @author Santiago Pericas-Geertsen
 */
public class BodyPart implements Flow.Subscriber<MultiPartDataChunk>, Flow.Publisher<DataChunk> {

    private MultiPart parent;
    private Content content;
    private BodyPartSubscription subscription = null;
    private Flow.Subscriber<? super DataChunk> subscriber = null;
    private BodyPartHeaders headers = null;
    private boolean complete = false;

    public BodyPart() {
    }

    BodyPart(MultiPart parent) {
        if (parent == null) {
            throw new IllegalArgumentException("Parent cannot be null");
        }
        this.parent = parent;
        this.content = new Request.Content(
                (Request) parent.request(), this);
    }

    public ServerRequest request() {
        return parent.request();
    }

    public ServerResponse response() {
        return parent.response();
    }

    public BodyPartHeaders headers() {
        if (headers == null) {
            throw new IllegalStateException(
                    "needs at least one processed chunk");
        }
        return headers;
    }

    public Content content() {
        return content;
    }

    @Override
    public void onSubscribe(final Flow.Subscription s) {
        if (subscription != null) {
            throw new IllegalStateException("Subscription is not null");
        }
        subscription = new BodyPartSubscription(s, parent.processor());
    }

    @Override
    public void onNext(final MultiPartDataChunk item) {
        checkComplete();
        checkSubscriber();
        checkSubscription();
        if (headers == null) {
            headers = item.headers();
        }
        subscriber.onNext(item);
        subscription.onDelivered();
    }

    @Override
    public void onError(final Throwable throwable) {
        checkComplete();
        if (subscriber == null) {
            throw new IllegalStateException("Subscriber is null", throwable);
        }
        subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        checkComplete();
        checkSubscriber();
        checkSubscription();
        complete = true;
        subscriber.onComplete();
        subscriber = null;
        parent.onBodyPartComplete();
        if (!parent.processor().isComplete()
                && !subscription.hasUndelivered()) {
            // if the current subscription has not requested enough to
            // receive the chunk for the next potential body part
            // request 1 more to make sure we can receive it
            subscription.request(1);
        }
    }

    @Override
    public void subscribe(final Flow.Subscriber<? super DataChunk> s) {
        if (subscriber != null) {
            throw new IllegalStateException("Already subscribed");
        }
        checkComplete();
        checkSubscription();
        subscriber = s;
        subscriber.onSubscribe(subscription);
    }

    /**
     * Cancel the subscription.
     *
     * @throws IllegalStateException if the subscription is {@code null}
     */
    void cancelSubscription() {
        checkSubscription();
        subscription.cancel();
    }

    /**
     * Check if this publisher is completed.
     *
     * @throws {@link IllegalStateException} if this publisher is already
     * completed
     */
    private void checkComplete() {
        if (complete) {
            throw new IllegalStateException("Already completed");
        }
    }

    /**
     * Check if this publisher has a subscriber.
     *
     * @throws {@link IllegalStateException} if this publisher does not
     * have a subscriber
     */
    private void checkSubscriber() {
        if (subscriber == null) {
            throw new IllegalStateException("Subscriber is null");
        }
    }

    /**
     * Check if this publisher has a subscription.
     *
     * @throws {@link IllegalStateException} if this publisher does not
     * have a subscription
     */
    private void checkSubscription() {
        if (subscription == null) {
            throw new IllegalArgumentException("Subscription is null");
        }
    }

    /**
     * Returns a builder for {@link MultiPart}.
     * @return
     */
    public static BodyPartBuilder builder(){
        return new BodyPartBuilder();
    }

    /**
     * Builder for {@link MultiPart}.
     */
    public static class BodyPartBuilder implements Builder<BodyPart> {

        @Override
        public BodyPart build() {
            return null;
        }

        @Override
        public BodyPart get() {
            return null;
        }
    }

    /**
     * A delegated subscription used to send the (cached) first chunk when
     * the content subscriber has requested data.
     * It keeps the count of requested and delivered items in order to indicate
     * if there are more items to be delivered, see {@link #hasUndelivered()},
     * {@link #onDelivered().
     */
    static class BodyPartSubscription implements Flow.Subscription {

        private final Flow.Subscription delegate;
        private final MultiPart.BodyPartProcessor processor;
        private long requested = 1;
        private long delivered = 0;
        private boolean canceled = false;

        /**
         * Create a new body part subscription.
         *
         * @param delegate the subscription to delegate
         * @param processor the body part processor
         */
        BodyPartSubscription(final Flow.Subscription delegate,
                             final MultiPart.BodyPartProcessor processor) {
            this.delegate = delegate;
            this.processor = processor;
        }

        /**
         * Check if this subscription expects more items.
         *
         * @return {@code true} if this subscription expects more items to be
         * delivered, {@code false} otherwise
         */
        boolean hasUndelivered() {
            return requested == Long.MAX_VALUE || requested > delivered;
        }

        /**
         * Increase the count of delivered items of this subscription.
         */
        void onDelivered() {
            delivered++;
        }

        @Override
        public void request(long n) {
            checkCanceled();
            long reqCount = n;
            if (n > 0) {
                if (delivered == 0) {
                    reqCount--;
                    requested += (reqCount);
                    processor.submitFirstChunk();
                } else {
                    requested += reqCount;
                }
                if (requested > delivered) {
                    delegate.request(reqCount);
                }
            }
        }

        @Override
        public void cancel() {
            checkCanceled();
            delegate.cancel();
            canceled = true;
        }

        /**
         * Check if this subscription is canceled.
         *
         * @throws {@link IllegalStateException} if this is subscription is
         * canceled
         */
        private void checkCanceled() {
            if (canceled) {
                throw new IllegalArgumentException("Subscription has been canceled");
            }
        }
    }
}

