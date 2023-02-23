package com.example.stockanalyzer;

import ru.tinkoff.piapi.contract.v1.Account;
import ru.tinkoff.piapi.core.InvestApi;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        var instrumentFigi = "BBG000N9MNX3";

//        var token = "t.PhcEc86HoApW_TIr1E_DqfUB4W1ttPNyN-O1Z0ck_CkcwPIUTLpcu0ifBXAk_7AsZ3TevIeAL1dr8ayytbfHBg";
        var token = "t.r1RjGdULtS1QIwg-k30CmN6RQA65yLseFYIckRXHgyQupL6Vcs0lvGLDsvhs1V3mGTDhKOJxwJ6HxbDyuygcuQ";
        var api = InvestApi.create(token);
        var a = api.getInstrumentsService().getInstrumentByFigi("BBG000N9MNX3");
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println(a.get().getName()+ " " + a.get().getTicker());

        Account id = api.getUserService().getAccounts().get().get(0);
        var b = api.getOperationsService().getPortfolio(id.getId()).get();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println(b.getPositions().get(0).getFigi() +"  " + b.getTotalAmountEtfs().getValue() + " " + b.getTotalAmountPortfolio().getValue());
    }
}

// TSLA ->  BBG000N9MNX3
// AAPL -> BBG000B9XRY4
// t.r1RjGdULtS1QIwg-k30CmN6RQA65yLseFYIckRXHgyQupL6Vcs0lvGLDsvhs1V3mGTDhKOJxwJ6HxbDyuygcuQ