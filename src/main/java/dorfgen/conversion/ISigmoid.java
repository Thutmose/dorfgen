package dorfgen.conversion;

public interface ISigmoid
{
    default int elevationSigmoid(int preHeight)
    {
        double a = preHeight;

        return (int) (Math.min(Math.max(((215. / (1 + Math.exp(-(a - 128.) / 20.))) + 45. + a) / 2., 10), 245));
    }
}
