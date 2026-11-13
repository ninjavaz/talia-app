package app.talia;

import org.springframework.boot.SpringApplication;

import java.util.Arrays;

/** Local dev: runs the app against a throwaway Postgres container. Login: talia / talia, sync token: test-token. */
public class TestTaliaApplication {

	public static void main(String[] args) {
		String[] withProfile = Arrays.copyOf(args, args.length + 1);
		withProfile[args.length] = "--spring.profiles.active=test";
		SpringApplication.from(TaliaApplication::main).with(TestcontainersConfiguration.class).run(withProfile);
	}

}
