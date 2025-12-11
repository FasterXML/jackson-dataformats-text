package tools.jackson.dataformat.yaml;

/**
 * A subclass of YAMLFactoryBuilder with the only purpose to replace the YAMLFactory by
 * the YAMLAnchorReplayingFactory subclass.
 */
public class YAMLAnchorReplayingFactoryBuilder extends YAMLFactoryBuilder {
	protected YAMLAnchorReplayingFactoryBuilder() {
		super();
	}

	public YAMLAnchorReplayingFactoryBuilder(YAMLAnchorReplayingFactory base) {
		super(base);
	}

	@Override
	public YAMLAnchorReplayingFactory build() {
		return new YAMLAnchorReplayingFactory(this);
	}
}
