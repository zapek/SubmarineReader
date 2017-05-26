$(function()
{
	var $body = $("body");

	/*
	 * Setup the night mode, if enabled.
	 */
	if (SubmarineReader.isNightModeEnabled())
	{
		$body.css("color", "#c1c1c1");
		$("a:link").css("color", "#00afff");
	}

	/*
	 * Resize iframes to the viewport size, preserving the aspect ratio
	 * if it seems to be a video.
	 */
	var $iframes = $("iframe");

	$iframes.each(function()
	{
		$(this).data('aspectRatio', this.height / this.width)
		.removeAttr('height')
		.removeAttr('width');
	});

	$(window).resize(function()
	{
		var newWidth = $body.width();

		$iframes.each(function() {
			var $iframe = $(this);

			if (($iframe.data('aspectRatio')) >= 0.4) /* 21x9 (cinemascope) */
			{
				$iframe
                	.width(newWidth)
                	.height(newWidth * $iframe.data('aspectRatio'));
			}
			else
			{
				$iframe.width(newWidth);
			}
		});
	}).resize();
});