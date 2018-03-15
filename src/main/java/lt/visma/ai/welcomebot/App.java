package lt.visma.ai.welcomebot;

public class App {

    public static void main(String[] args) throws Exception {
	String mode = "normal";

	if (args.length > 0)
		mode = args[0];

	System.out.println("mode is "+mode);

	if (!mode.equals("normal") && !mode.equals("--training"))
		mode = "normal";

	boolean trainingMode = mode.equals("--training");

        new MainFrame(trainingMode);
    }

}
