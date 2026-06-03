package farvix.solution.api.events;

import lombok.Getter;
import lombok.Setter;
import farvix.solution.Client;

@Getter @Setter
public class Event {
    boolean cancelled;

    @SuppressWarnings("unchecked")
    public <T extends Event> T call() {
        Client.getInstance().getBus().post(this);
        return (T) this;
    }
}
