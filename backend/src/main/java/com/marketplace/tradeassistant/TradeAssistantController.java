package com.marketplace.tradeassistant;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import static com.marketplace.tradeassistant.TradeAssistantDtos.*;
@RestController @RequestMapping("/api/trade/assistant") @lombok.RequiredArgsConstructor
public class TradeAssistantController {
    private final TradeAssistantService service;
    @GetMapping("/conversation") public View get() { return service.get(); }
    @PostMapping("/messages") public View reply(@Valid @RequestBody Input input) { return service.reply(input); }
}
